const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { google } = require("googleapis");
const {
  mapSubscriptionState,
  isOwnerAuthorized,
  validateTokenBinding,
  validatePromoCode,
} = require("./lib/licenseLogic");

admin.initializeApp();
const db = admin.firestore();

const DEFAULT_PACKAGE_NAME = "com.mmushtaq04.buysell";

/**
 * Helper to query Google Play AndroidPublisher v3 subscriptionsv2.get
 */
async function queryPlaySubscriptionV2(token) {
  const auth = new google.auth.GoogleAuth({
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
  const androidPublisher = google.androidpublisher({ version: "v3", auth });

  const response = await androidPublisher.purchases.subscriptionsv2.get({
    token: token,
  });

  return response.data;
}

/**
 * 1. verifyPlayPurchase (Callable Cloud Function)
 * T1-T6: Hardened Play Purchase Verification
 */
exports.verifyPlayPurchase = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The user must be authenticated to verify a purchase."
    );
  }

  const { purchaseToken, productId, shopId } = data;
  const targetPackageName = data.packageName || DEFAULT_PACKAGE_NAME;

  if (!purchaseToken || !productId || !shopId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Missing required fields: purchaseToken, productId, or shopId."
    );
  }

  functions.logger.info(`Verifying purchase token for shop: ${shopId}, product: ${productId}`);

  // T3: Authorization check - Caller must be shop owner
  const shopSnap = await db.collection("shops").doc(shopId).get();
  if (!shopSnap.exists) {
    throw new functions.https.HttpsError("not-found", `Shop '${shopId}' does not exist.`);
  }

  const shopData = shopSnap.data();
  if (!isOwnerAuthorized(context.auth.uid, shopData.owner_user_id)) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only the shop owner can verify purchases for this shop."
    );
  }

  // T4: Token binding check - Verify purchase token is not bound to a different shop
  const purchaseSnap = await db.collection("purchases").doc(purchaseToken).get();
  const existingPurchase = purchaseSnap.exists ? purchaseSnap.data() : null;

  let playData = null;

  // T2 & T5: Fail closed. Query Google Play subscriptionsv2 API
  try {
    playData = await queryPlaySubscriptionV2(purchaseToken);
  } catch (apiError) {
    functions.logger.error(`Google Play API verification failed for token '${purchaseToken}': ${apiError.message}`);
    throw new functions.https.HttpsError(
      "failed-precondition",
      `Google Play purchase verification failed: ${apiError.message}`
    );
  }

  if (!playData) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Empty response received from Google Play purchase verification."
    );
  }

  // T4: Check obfuscatedExternalAccountId if present
  const extAccounts = playData.externalAccountIdentifiers || null;
  const bindingCheck = validateTokenBinding(existingPurchase, shopId, extAccounts);
  if (!bindingCheck.valid) {
    functions.logger.error(`Token binding failed: ${bindingCheck.reason}`);
    throw new functions.https.HttpsError("permission-denied", bindingCheck.reason);
  }

  if (!extAccounts || !extAccounts.obfuscatedExternalAccountId) {
    functions.logger.warn(`No obfuscatedExternalAccountId present in Play response for shop '${shopId}'`);
  }

  // T5: Map subscription state from Play response
  const mapped = mapSubscriptionState(playData, productId);
  const nowMs = Date.now();

  const batch = db.batch();

  // T6: Write trusted license document to shops/{shopId}/license/current
  const licenseRef = db.collection("shops").doc(shopId).collection("license").doc("current");
  batch.set(
    licenseRef,
    {
      plan: mapped.productId || productId,
      status: mapped.status,
      valid_until: mapped.validUntilMs,
      source: "PLAY",
      auto_renewing: mapped.autoRenewing,
      server_time_ms: nowMs,
      updated_at: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  // T6: Audit log purchase record in purchases/{purchaseToken} (server-only)
  const purchaseRef = db.collection("purchases").doc(purchaseToken);
  batch.set(
    purchaseRef,
    {
      shop_id: shopId,
      user_uid: context.auth.uid,
      product_id: mapped.productId || productId,
      package_name: targetPackageName,
      purchase_token: purchaseToken,
      status: mapped.status,
      valid_until: mapped.validUntilMs,
      auto_renewing: mapped.autoRenewing,
      verified_at: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  await batch.commit();

  functions.logger.info(`✓ Successfully verified purchase for shop ${shopId}. Status: ${mapped.status}, Valid until: ${new Date(mapped.validUntilMs).toISOString()}`);

  return {
    success: true,
    status: mapped.status,
    plan: mapped.productId || productId,
    validUntil: mapped.validUntilMs,
    autoRenewing: mapped.autoRenewing,
  };
});

/**
 * 2. handlePlayRtdn (Pub/Sub Triggered / Authenticated Cloud Function)
 * T7: Handles Google Play RTDN lifecycle notifications securely.
 */
exports.handlePlayRtdn = functions.pubsub.topic("play-rtdn").onPublish(async (message) => {
  try {
    const rawData = message.data ? Buffer.from(message.data, "base64").toString("utf-8") : null;
    if (!rawData) {
      functions.logger.warn("Received empty Pub/Sub message data in RTDN handler");
      return null;
    }

    const rtdnPayload = JSON.parse(rawData);
    functions.logger.info("Received Google Play RTDN event via Pub/Sub:", rtdnPayload);

    const { subscriptionNotification } = rtdnPayload;
    if (!subscriptionNotification) {
      functions.logger.info("Non-subscription RTDN notification received. Ignoring.");
      return null;
    }

    const { notificationType, purchaseToken, subscriptionId } = subscriptionNotification;
    functions.logger.info(`RTDN Event Type: ${notificationType} for token: ${purchaseToken}`);

    // Lookup purchase record in purchases/{purchaseToken}
    const purchaseSnap = await db.collection("purchases").doc(purchaseToken).get();
    if (!purchaseSnap.exists) {
      functions.logger.warn(`No purchase record found in purchases collection for token: ${purchaseToken}`);
      return null;
    }

    const purchaseData = purchaseSnap.data();
    const shopId = purchaseData.shop_id;
    if (!shopId) {
      functions.logger.warn(`No shop_id bound to purchase token: ${purchaseToken}`);
      return null;
    }

    // T7: Re-query Google Play API for authoritative status
    let playData = null;
    try {
      playData = await queryPlaySubscriptionV2(purchaseToken);
    } catch (apiErr) {
      functions.logger.error(`Error re-querying Play API in RTDN handler for token '${purchaseToken}': ${apiErr.message}`);
      return null;
    }

    const mapped = mapSubscriptionState(playData, subscriptionId || purchaseData.product_id);
    const nowMs = Date.now();

    const licenseRef = db.collection("shops").doc(shopId).collection("license").doc("current");
    await licenseRef.set(
      {
        plan: mapped.productId || subscriptionId || purchaseData.product_id,
        status: mapped.status,
        valid_until: mapped.validUntilMs,
        source: "PLAY",
        auto_renewing: mapped.autoRenewing,
        server_time_ms: nowMs,
        updated_at: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    functions.logger.info(`✓ RTDN updated license for shop ${shopId}: status=${mapped.status}, valid_until=${new Date(mapped.validUntilMs).toISOString()}`);
    return null;
  } catch (error) {
    functions.logger.error("Error processing Google Play RTDN event:", error);
    // Return null so function finishes gracefully and Pub/Sub does not retry endlessly for invalid payloads
    return null;
  }
});

/**
 * 3. redeemPromoCode (Callable Cloud Function)
 * T8: Secure Promo Code Redemption
 */
exports.redeemPromoCode = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The user must be authenticated to redeem a promo code."
    );
  }

  const { code, shopId } = data;
  if (!code || !shopId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Missing required fields: code or shopId."
    );
  }

  const promoCode = code.trim().toUpperCase();

  // Authorization check - Caller must be shop owner
  const shopSnap = await db.collection("shops").doc(shopId).get();
  if (!shopSnap.exists) {
    throw new functions.https.HttpsError("not-found", `Shop '${shopId}' does not exist.`);
  }

  const shopData = shopSnap.data();
  if (!isOwnerAuthorized(context.auth.uid, shopData.owner_user_id)) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only the shop owner can redeem promo codes for this shop."
    );
  }

  const nowMs = Date.now();

  try {
    const result = await db.runTransaction(async (transaction) => {
      const promoRef = db.collection("app_config/admin_promos/codes").doc(promoCode);
      const redemptionRef = db.collection("promo_redemptions").doc(`${promoCode}_${shopId}`);
      const licenseRef = db.collection("shops").doc(shopId).collection("license").doc("current");

      const promoDoc = await transaction.get(promoRef);
      const redemptionDoc = await transaction.get(redemptionRef);

      const validation = validatePromoCode(
        promoDoc.exists ? promoDoc.data() : null,
        redemptionDoc.exists,
        nowMs
      );

      if (!validation.valid) {
        throw new Error(validation.reason);
      }

      const grantMs = validation.grantDays * 24 * 60 * 60 * 1000;
      const validUntilMs = nowMs + grantMs;

      // 1. Write license doc shops/{shopId}/license/current
      transaction.set(
        licenseRef,
        {
          plan: promoCode,
          status: "ACTIVE",
          valid_until: validUntilMs,
          source: "PROMO",
          auto_renewing: false,
          server_time_ms: nowMs,
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );

      // 2. Increment redeemed_count on promo code doc
      transaction.update(promoRef, {
        redeemed_count: validation.redeemedCount + 1,
        updated_at: admin.firestore.FieldValue.serverTimestamp(),
      });

      // 3. Record redemption proof in promo_redemptions/{CODE}_{shopId}
      transaction.set(redemptionRef, {
        code: promoCode,
        shop_id: shopId,
        redeemed_by: context.auth.uid,
        redeemed_at: admin.firestore.FieldValue.serverTimestamp(),
      });

      return {
        success: true,
        plan: promoCode,
        validUntil: validUntilMs,
      };
    });

    functions.logger.info(`✓ Successfully redeemed promo code '${promoCode}' for shop '${shopId}' until ${new Date(result.validUntil).toISOString()}`);
    return result;
  } catch (err) {
    functions.logger.error(`Error redeeming promo code '${promoCode}' for shop '${shopId}': ${err.message}`);
    throw new functions.https.HttpsError(
      "failed-precondition",
      `Promo code redemption failed: ${err.message}`
    );
  }
});
