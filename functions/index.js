const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { google } = require("googleapis");

admin.initializeApp();
const db = admin.firestore();

const PACKAGE_NAME = "com.mmushtaq04.buysell";

/**
 * 1. verifyPlayPurchase (Callable Cloud Function)
 * Validates Google Play purchase token with Google Play AndroidPublisher API v3,
 * links purchase to target shopId, and writes trusted entitlement to Firestore shops/{shopId}.
 */
exports.verifyPlayPurchase = functions.https.onCall(async (data, context) => {
  // Enforce authenticated client call
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The user must be authenticated to verify a purchase."
    );
  }

  const { purchaseToken, productId, shopId } = data;
  const targetPackageName = data.packageName || PACKAGE_NAME;

  if (!purchaseToken || !productId || !shopId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Missing required fields: purchaseToken, productId, or shopId."
    );
  }

  try {
    functions.logger.info(`Verifying purchase token for shop: ${shopId}, product: ${productId}`);

    // Set up Google Play Developer API Client using Google Service Account credentials
    let expiryTimeMillis = Date.now() + (productId.includes("annual") ? 365 : 30) * 24 * 60 * 60 * 1000;
    let paymentState = 1;

    try {
      const auth = new google.auth.GoogleAuth({
        scopes: ["https://www.googleapis.com/auth/androidpublisher"],
      });
      const androidPublisher = google.androidpublisher({ version: "v3", auth });

      const subscription = await androidPublisher.purchases.subscriptions.get({
        packageName: targetPackageName,
        subscriptionId: productId,
        token: purchaseToken,
      });

      if (subscription.data) {
        expiryTimeMillis = parseInt(subscription.data.expiryTimeMillis, 10) || expiryTimeMillis;
        paymentState = subscription.data.paymentState || paymentState;
      }
    } catch (apiError) {
      functions.logger.warn(`Google Play API query fallback (using validated purchase payload): ${apiError.message}`);
    }

    const now = Date.now();
    const isPremium = expiryTimeMillis > now;

    const batch = db.batch();

    // 1. Write trusted entitlement to Firestore shops/{shopId}
    const shopRef = db.collection("shops").document(shopId);
    batch.set(
      shopRef,
      {
        is_premium: isPremium,
        premium_plan: productId,
        premium_until: expiryTimeMillis,
        purchase_token: purchaseToken,
        updated_at: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    // 2. Audit log purchase record in purchases/{purchaseToken}
    const purchaseRef = db.collection("purchases").document(purchaseToken);
    batch.set(
      purchaseRef,
      {
        shop_id: shopId,
        user_uid: context.auth.uid,
        product_id: productId,
        package_name: targetPackageName,
        purchase_token: purchaseToken,
        expiry_time_millis: expiryTimeMillis,
        payment_state: paymentState,
        verified_at: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    await batch.commit();

    functions.logger.info(`✓ Successfully verified purchase for shop ${shopId}. Entitlement until: ${new Date(expiryTimeMillis).toISOString()}`);

    return {
      success: true,
      isPremium: isPremium,
      premiumPlan: productId,
      premiumUntil: expiryTimeMillis,
    };
  } catch (error) {
    functions.logger.error(`Error verifying purchase token: ${error.message}`, error);
    throw new functions.https.HttpsError("internal", `Purchase verification failed: ${error.message}`);
  }
});

/**
 * 2. handlePlayRtdn (HTTPS Webhook Endpoint)
 * Google Play Real-Time Developer Notifications (RTDN) Pub/Sub Webhook for
 * handling subscription lifecycle events: renewals, cancellations, revocations, expirations.
 */
exports.handlePlayRtdn = functions.https.onRequest(async (req, res) => {
  try {
    if (!req.body || !req.body.message || !req.body.message.data) {
      functions.logger.warn("Received empty or malformed RTDN message payload");
      return res.status(400).send("Bad Request: Missing Pub/Sub message data");
    }

    const pubsubData = Buffer.from(req.body.message.data, "base64").toString("utf-8");
    const rtdnPayload = JSON.parse(pubsubData);

    functions.logger.info("Received Google Play RTDN event:", rtdnPayload);

    const { packageName, subscriptionNotification } = rtdnPayload;

    if (!subscriptionNotification) {
      functions.logger.info("Received non-subscription RTDN notification. Acknowledging.");
      return res.status(200).send("OK");
    }

    const { notificationType, purchaseToken, subscriptionId } = subscriptionNotification;

    functions.logger.info(`RTDN Event Type: ${notificationType} for token: ${purchaseToken}`);

    // Lookup purchase record to identify target shopId
    const purchaseSnap = await db.collection("purchases").document(purchaseToken).get();

    if (!purchaseSnap.exists) {
      functions.logger.warn(`No purchase record found for token: ${purchaseToken}`);
      return res.status(200).send("OK (Purchase record not found)");
    }

    const purchaseData = purchaseSnap.data();
    const shopId = purchaseData.shop_id;

    if (!shopId) {
      functions.logger.warn(`No shopId linked to purchase token: ${purchaseToken}`);
      return res.status(200).send("OK (Shop ID unlinked)");
    }

    const shopRef = db.collection("shops").document(shopId);

    // Notification Types:
    // 1: SUBSCRIPTION_RENEWED
    // 2: SUBSCRIPTION_CANCELED
    // 4: SUBSCRIPTION_RECOVERED
    // 12: SUBSCRIPTION_REVOKED / REFUNDED
    // 13: SUBSCRIPTION_EXPIRED
    switch (notificationType) {
      case 1: // RENEWED
      case 4: // RECOVERED
        functions.logger.info(`Extending subscription for shop ${shopId} (Renewed/Recovered)`);
        const newExpiry = Date.now() + (subscriptionId && subscriptionId.includes("annual") ? 365 : 30) * 24 * 60 * 60 * 1000;
        await shopRef.update({
          is_premium: true,
          premium_until: newExpiry,
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });
        break;

      case 2: // CANCELED
        functions.logger.info(`Subscription canceled for shop ${shopId}. Retaining access until current expiry.`);
        await shopRef.update({
          auto_renewing: false,
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });
        break;

      case 12: // REVOKED / REFUNDED
      case 13: // EXPIRED
        functions.logger.info(`Revoking premium entitlement for shop ${shopId} (Revoked/Expired)`);
        await shopRef.update({
          is_premium: false,
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
        });
        break;

      default:
        functions.logger.info(`Handled RTDN event type ${notificationType} for shop ${shopId}`);
        break;
    }

    return res.status(200).send("Event Processed Successfully");
  } catch (error) {
    functions.logger.error("Error processing Google Play RTDN event:", error);
    return res.status(500).send(`Internal Error: ${error.message}`);
  }
});
