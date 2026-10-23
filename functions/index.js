const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { google } = require("googleapis");
const {
  hashToken,
  shouldUpdateLicense,
  computePromoExtension,
  mapSubscriptionState,
  isOwnerAuthorized,
  validateTokenBinding,
  validatePromoCode,
} = require("./lib/licenseLogic");

admin.initializeApp();
const db = admin.firestore();

const DEFAULT_PACKAGE_NAME = "com.mmushtaq04.buysell";
const SUBSCRIPTION_IDS = ["buysell-pro-monthly", "buysell-pro-annual"];

// Reusable Auth Client
const auth = new google.auth.GoogleAuth({
  scopes: ["https://www.googleapis.com/auth/androidpublisher"],
});
const androidPublisher = google.androidpublisher({ version: "v3", auth });

/**
 * Helper: Safe Acknowledge
 * Ignores 400 errors (meaning it was already acknowledged).
 */
async function safelyAcknowledge(type, packageName, productId, token) {
  try {
    if (type === "subscription") {
      await androidPublisher.purchases.subscriptions.acknowledge({
        packageName,
        subscriptionId: productId,
        token,
      });
    } else {
      await androidPublisher.purchases.products.acknowledge({
        packageName,
        productId,
        token,
      });
    }
    functions.logger.info(`✓ Acknowledged ${type} for token: ${token}`);
  } catch (error) {
    // 400 means it was already acknowledged, which is fine.
    if (error.code !== 400) {
      functions.logger.error(`Failed to acknowledge ${type} for token ${token}:`, error.message);
      throw error; // Fail the function so the client knows to retry
    }
  }
}

/**
 * 1. verifyPlayPurchase (Callable Cloud Function)
 * Handles both Subscriptions and One-Time Products.
 */
exports.verifyPlayPurchase = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "The user must be authenticated.");
  }

  const { purchaseToken, productId, shopId } = data;
  const targetPackageName = data.packageName || DEFAULT_PACKAGE_NAME;

  if (!purchaseToken || !productId || !shopId) {
    throw new functions.https.HttpsError("invalid-argument", "Missing required fields.");
  }

  functions.logger.info(`Verifying purchase token for shop: ${shopId}, product: ${productId}`);

  // 1. Authorization check - Caller must be shop owner
  const shopSnap = await db.collection("shops").doc(shopId).get();
  if (!shopSnap.exists) {
    throw new functions.https.HttpsError("not-found", `Shop '${shopId}' does not exist.`);
  }

  if (!isOwnerAuthorized(context.auth.uid, shopSnap.data().owner_user_id)) {
    throw new functions.https.HttpsError("permission-denied", "Only the shop owner can verify purchases.");
  }

  // 2. Query Google Play API (Fail closed)
  let playData = null;
  const isSubscription = SUBSCRIPTION_IDS.includes(productId);

  try {
    if (isSubscription) {
      const res = await androidPublisher.purchases.subscriptionsv2.get({
        packageName: targetPackageName,
        token: purchaseToken,
      });
      playData = res.data;
    } else {
      const res = await androidPublisher.purchases.products.get({
        packageName: targetPackageName,
        productId: productId,
        token: purchaseToken,
      });
      playData = res.data;
    }
  } catch (apiError) {
    functions.logger.error(`Play API verification failed: ${apiError.message}`);
    throw new functions.https.HttpsError("failed-precondition", `Verification failed: ${apiError.message}`);
  }

  // 3. Handle One-Time Products (Coffee Tip)
  if (!isSubscription) {
    // purchaseState 0 means Purchased
    if (playData.purchaseState !== 0) {
      throw new functions.https.HttpsError("failed-precondition", "One-time product is not in a valid purchased state.");
    }

    // Acknowledge immediately so it isn't refunded
    await safelyAcknowledge("product", targetPackageName, productId, purchaseToken);

    // Note: You might want to log this to Firestore, but it doesn't grant Pro access.
    await db.collection("purchases").doc(purchaseToken).set({
      shop_id: shopId,
      user_uid: context.auth.uid,
      product_id: productId,
      package_name: targetPackageName,
      status: "PURCHASED",
      verified_at: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });

    return { success: true, type: "one-time", status: "PURCHASED" };
  }

  // 4. Handle Subscriptions & Token Binding Validation
  const extAccounts = playData.externalAccountIdentifiers || null;

  const purchaseSnap = await db.collection("purchases").doc(purchaseToken).get();
  const existingPurchaseDoc = purchaseSnap.exists ? purchaseSnap.data() : null;

  const bindingResult = validateTokenBinding(existingPurchaseDoc, shopId, extAccounts);
  if (!bindingResult.valid) {
    functions.logger.error(`Token binding validation failed for shop '${shopId}': ${bindingResult.reason}`);
    throw new functions.https.HttpsError("permission-denied", bindingResult.reason);
  }

  // 5. Map State and Acknowledge
  const mapped = mapSubscriptionState(playData, productId);

  // Only acknowledge if it's active or in grace period
  if (mapped.status === "ACTIVE" || mapped.status === "IN_GRACE_PERIOD") {
    await safelyAcknowledge("subscription", targetPackageName, productId, purchaseToken);
  }

  // 6. Write to Firestore
  const nowMs = Date.now();
  const tokenHash = hashToken(purchaseToken);
  const licenseRef = db.collection("shops").doc(shopId).collection("license").doc("current");
  const purchaseRef = db.collection("purchases").doc(purchaseToken);

  const currentLicenseSnap = await licenseRef.get();
  const currentLicenseData = currentLicenseSnap.exists ? currentLicenseSnap.data() : null;

  const canUpdate = shouldUpdateLicense(currentLicenseData, tokenHash, mapped.validUntilMs, nowMs);

  const batch = db.batch();

  if (canUpdate) {
    batch.set(licenseRef, {
      plan: mapped.productId || productId,
      status: mapped.status,
      valid_until: mapped.validUntilMs,
      source: "PLAY",
      owning_token_hash: tokenHash,
      auto_renewing: mapped.autoRenewing,
      server_time_ms: nowMs,
      updated_at: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  } else {
    functions.logger.info(`verifyPlayPurchase: Skipped license update for shop '${shopId}' as current license is active and owned by a newer token.`);
  }

  batch.set(purchaseRef, {
    shop_id: shopId,
    user_uid: context.auth.uid,
    product_id: mapped.productId || productId,
    package_name: targetPackageName,
    status: mapped.status,
    valid_until: mapped.validUntilMs,
    auto_renewing: mapped.autoRenewing,
    verified_at: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });

  await batch.commit();

  return {
    success: true,
    type: "subscription",
    status: mapped.status,
    plan: mapped.productId || productId,
    validUntil: mapped.validUntilMs,
    autoRenewing: mapped.autoRenewing,
  };
});

/**
 * 2. handlePlayRtdn (Pub/Sub Triggered)
 * Fixes the Race Condition: Can handle notifications even if the client never
 * called verifyPlayPurchase (because it reads the shopId from the Play API).
 */
exports.handlePlayRtdn = functions.pubsub.topic("play-rtdn").onPublish(async (message) => {
  try {
    const rawData = message.data ? Buffer.from(message.data, "base64").toString("utf-8") : null;
    if (!rawData) {
      functions.logger.warn("Empty Pub/Sub message.");
      return null;
    }

    const rtdnPayload = JSON.parse(rawData);

    // Ignore test notifications
    if (rtdnPayload.testNotification) {
      functions.logger.info("Test notification received successfully.");
      return null;
    }

    const subNotif = rtdnPayload.subscriptionNotification;
    const oneTimeNotif = rtdnPayload.oneTimeProductNotification;

    if (!subNotif && !oneTimeNotif) {
      functions.logger.info("Unknown notification type. Ignoring.");
      return null;
    }

    const purchaseToken = subNotif ? subNotif.purchaseToken : oneTimeNotif.purchaseToken;
    const productId = subNotif ? subNotif.subscriptionId : oneTimeNotif.sku;
    const isSubscription = !!subNotif;

    functions.logger.info(`RTDN received for ${productId}, token: ${purchaseToken}`);

    // 1. Fetch Authoritative Data from Play API
    let playData = null;
    try {
      if (isSubscription) {
        const res = await androidPublisher.purchases.subscriptionsv2.get({
          packageName: DEFAULT_PACKAGE_NAME,
          token: purchaseToken,
        });
        playData = res.data;
      } else {
        const res = await androidPublisher.purchases.products.get({
          packageName: DEFAULT_PACKAGE_NAME,
          productId: productId,
          token: purchaseToken,
        });
        playData = res.data;
      }
    } catch (apiErr) {
      functions.logger.error(`Play API failed in RTDN: ${apiErr.message}`);
      throw new Error("Play API failed"); // Throw to trigger Pub/Sub retry
    }

    // 2. Find the Shop ID
    // Try the Firestore doc first (faster). If missing, use Play API's obfuscatedExternalAccountId.
    const purchaseSnap = await db.collection("purchases").doc(purchaseToken).get();
    let shopId = purchaseSnap.exists ? purchaseSnap.data().shop_id : null;

    if (!shopId && playData.externalAccountIdentifiers) {
      shopId = playData.externalAccountIdentifiers.obfuscatedExternalAccountId;
      functions.logger.info(`Race condition resolved. Found shopId ${shopId} from Play API.`);
    }

    if (!shopId) {
      functions.logger.warn(`No shop_id found for token ${purchaseToken}. Cannot update license.`);
      return null; // Can't do anything without a shopId
    }

    // 3. Update Firestore
    const nowMs = Date.now();
    const batch = db.batch();

    const purchaseRef = db.collection("purchases").doc(purchaseToken);

    if (isSubscription) {
      const mapped = mapSubscriptionState(playData, productId);
      const tokenHash = hashToken(purchaseToken);

      const licenseRef = db.collection("shops").doc(shopId).collection("license").doc("current");
      const currentLicenseSnap = await licenseRef.get();
      const currentLicenseData = currentLicenseSnap.exists ? currentLicenseSnap.data() : null;

      const canUpdate = shouldUpdateLicense(currentLicenseData, tokenHash, mapped.validUntilMs, nowMs);

      if (canUpdate) {
        batch.set(licenseRef, {
          plan: mapped.productId || productId,
          status: mapped.status,
          valid_until: mapped.validUntilMs,
          source: "PLAY",
          owning_token_hash: tokenHash,
          auto_renewing: mapped.autoRenewing,
          server_time_ms: nowMs,
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });
        functions.logger.info(`✓ RTDN updated license for shop ${shopId}: status=${mapped.status}`);
      } else {
        functions.logger.info(`RTDN ignored update for shop ${shopId}: token '${purchaseToken.slice(0, 10)}...' cannot overwrite active license owned by newer/other token.`);
      }

      batch.set(purchaseRef, {
        shop_id: shopId,
        product_id: productId,
        status: mapped.status,
        valid_until: mapped.validUntilMs,
        auto_renewing: mapped.autoRenewing,
        updated_at: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });

      functions.logger.info(`✓ RTDN updated license for shop ${shopId}: status=${mapped.status}`);
    } else {
      // One-time product (Coffee)
      batch.set(purchaseRef, {
        shop_id: shopId,
        product_id: productId,
        status: playData.purchaseState === 0 ? "PURCHASED" : "CANCELED",
        updated_at: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }

    await batch.commit();
    return null;

  } catch (error) {
    functions.logger.error("Error processing RTDN:", error);
    // THROWING HERE forces Pub/Sub to retry the message.
    throw new Error("RTDN Processing Failed");
  }
});

/**
 * 3. redeemPromoCode (Callable Cloud Function)
 * Kept as is - it was perfect.
 */
exports.redeemPromoCode = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
  }

  const { code, shopId } = data;
  if (!code || !shopId) {
    throw new functions.https.HttpsError("invalid-argument", "Missing code or shopId.");
  }

  const promoCode = code.trim().toUpperCase();
  const shopSnap = await db.collection("shops").doc(shopId).get();

  if (!shopSnap.exists) {
    throw new functions.https.HttpsError("not-found", `Shop does not exist.`);
  }
  if (!isOwnerAuthorized(context.auth.uid, shopSnap.data().owner_user_id)) {
    throw new functions.https.HttpsError("permission-denied", "Only the shop owner can redeem codes.");
  }

  const nowMs = Date.now();

  try {
    const result = await db.runTransaction(async (transaction) => {
      const promoRef = db.collection("app_config/admin_promos/codes").doc(promoCode);
      const redemptionRef = db.collection("promo_redemptions").doc(`${promoCode}_${shopId}`);
      const licenseRef = db.collection("shops").doc(shopId).collection("license").doc("current");

      const promoDoc = await transaction.get(promoRef);
      const redemptionDoc = await transaction.get(redemptionRef);
      const licenseDoc = await transaction.get(licenseRef);

      const validation = validatePromoCode(promoDoc.exists ? promoDoc.data() : null, redemptionDoc.exists, nowMs);
      if (!validation.valid) throw new Error(validation.reason);

      const currentLicenseData = licenseDoc.exists ? licenseDoc.data() : null;
      const extension = computePromoExtension(currentLicenseData, promoCode, shopId, validation.grantDays, nowMs);

      transaction.set(licenseRef, {
        plan: extension.plan,
        status: extension.status,
        valid_until: extension.validUntilMs,
        source: extension.source,
        owning_token_hash: extension.owningTokenHash,
        auto_renewing: extension.autoRenewing,
        server_time_ms: nowMs,
        updated_at: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });

      transaction.update(promoRef, {
        redeemed_count: validation.redeemedCount + 1,
        updated_at: admin.firestore.FieldValue.serverTimestamp(),
      });

      transaction.set(redemptionRef, {
        code: promoCode,
        shop_id: shopId,
        redeemed_by: context.auth.uid,
        redeemed_at: admin.firestore.FieldValue.serverTimestamp(),
      });

      return { success: true, plan: extension.plan, validUntil: extension.validUntilMs };
    });

    return result;
  } catch (err) {
    throw new functions.https.HttpsError("failed-precondition", err.message);
  }
});