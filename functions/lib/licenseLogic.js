/**
 * Pure business logic helpers for Cloud Functions hardening (T1 - T8).
 */

/**
 * Maps Google Play AndroidPublisher v3 SubscriptionPurchaseV2 payload to license status and metadata.
 * @param {object} subscriptionV2 - The response data from subscriptionsv2.get
 * @param {string} fallbackProductId - The default productId/plan if not in lineItems
 * @returns {object} { status, validUntilMs, autoRenewing, productId }
 */
function mapSubscriptionState(subscriptionV2, fallbackProductId) {
  if (!subscriptionV2) {
    return {
      status: "EXPIRED",
      validUntilMs: 0,
      autoRenewing: false,
      productId: fallbackProductId || "",
    };
  }

  const { subscriptionState, lineItems } = subscriptionV2;
  const primaryItem = (lineItems && lineItems.length > 0) ? lineItems[0] : null;

  const productId = primaryItem?.productId || fallbackProductId || "";
  const expiryTimeStr = primaryItem?.expiryTime;
  const validUntilMs = expiryTimeStr ? Date.parse(expiryTimeStr) : 0;
  const autoRenewing = Boolean(primaryItem?.autoRenewingPlan?.autoRenewEnabled);

  const nowMs = Date.now();

  let status = "EXPIRED";

  // Google Play subscriptionsv2 subscriptionState enum strings:
  // SUBSCRIPTION_STATE_UNSPECIFIED, SUBSCRIPTION_STATE_PENDING, SUBSCRIPTION_STATE_ACTIVE,
  // SUBSCRIPTION_STATE_PAUSED, SUBSCRIPTION_STATE_IN_GRACE_PERIOD, SUBSCRIPTION_STATE_ON_HOLD,
  // SUBSCRIPTION_STATE_CANCELED, SUBSCRIPTION_STATE_EXPIRED, SUBSCRIPTION_STATE_REVOKED
  switch (subscriptionState) {
    case "SUBSCRIPTION_STATE_ACTIVE":
      status = "ACTIVE";
      break;

    case "SUBSCRIPTION_STATE_IN_GRACE_PERIOD":
      status = "GRACE";
      break;

    case "SUBSCRIPTION_STATE_ON_HOLD":
    case "SUBSCRIPTION_STATE_PAUSED":
      status = "ON_HOLD";
      break;

    case "SUBSCRIPTION_STATE_CANCELED":
      if (validUntilMs > nowMs) {
        status = "CANCELED_ACTIVE";
      } else {
        status = "EXPIRED";
      }
      break;

    case "SUBSCRIPTION_STATE_EXPIRED":
    case "SUBSCRIPTION_STATE_REVOKED":
    case "SUBSCRIPTION_STATE_PENDING":
    case "SUBSCRIPTION_STATE_UNSPECIFIED":
    default:
      if (validUntilMs > nowMs && subscriptionState !== "SUBSCRIPTION_STATE_REVOKED" && subscriptionState !== "SUBSCRIPTION_STATE_EXPIRED") {
        status = "ACTIVE";
      } else {
        status = "EXPIRED";
      }
      break;
  }

  return {
    status,
    validUntilMs: isNaN(validUntilMs) ? 0 : validUntilMs,
    autoRenewing,
    productId,
  };
}

/**
 * Validates that caller is the owner of shopId.
 * @param {string|null} callerUid
 * @param {string|null} ownerUserId
 * @returns {boolean}
 */
function isOwnerAuthorized(callerUid, ownerUserId) {
  if (!callerUid || !ownerUserId) return false;
  return callerUid === ownerUserId;
}

/**
 * Validates purchase token binding.
 * @param {object|null} existingPurchaseDoc - Data from purchases/{purchaseToken} if exists
 * @param {string} targetShopId - The shopId requesting token binding
 * @param {object|null} externalAccountIdentifiers - From Play response
 * @returns {{ valid: boolean, reason?: string }}
 */
function validateTokenBinding(existingPurchaseDoc, targetShopId, externalAccountIdentifiers) {
  if (existingPurchaseDoc && existingPurchaseDoc.shop_id && existingPurchaseDoc.shop_id !== targetShopId) {
    return {
      valid: false,
      reason: `Purchase token is already bound to shop '${existingPurchaseDoc.shop_id}'`,
    };
  }

  if (externalAccountIdentifiers && externalAccountIdentifiers.obfuscatedExternalAccountId) {
    const obfId = externalAccountIdentifiers.obfuscatedExternalAccountId;
    if (obfId !== targetShopId) {
      return {
        valid: false,
        reason: `Obfuscated external account ID '${obfId}' does not match target shop '${targetShopId}'`,
      };
    }
  }

  return { valid: true };
}

/**
 * Validates promo code redemption rules inside transaction.
 * @param {object|null} promoData - Data from app_config/admin_promos/codes/{CODE}
 * @param {boolean} redemptionDocExists - True if promo_redemptions/{CODE}_{shopId} exists
 * @param {number} nowMs - Current time in ms
 * @returns {{ valid: boolean, reason?: string, grantDays?: number, maxRedemptions?: number, redeemedCount?: number }}
 */
function validatePromoCode(promoData, redemptionDocExists, nowMs) {
  if (!promoData) {
    return { valid: false, reason: "Promo code does not exist." };
  }

  if (promoData.active !== true) {
    return { valid: false, reason: "Promo code is inactive." };
  }

  if (promoData.expires_at) {
    let expiryMs = 0;
    if (typeof promoData.expires_at === "number") {
      expiryMs = promoData.expires_at;
    } else if (typeof promoData.expires_at.toMillis === "function") {
      expiryMs = promoData.expires_at.toMillis();
    } else if (typeof promoData.expires_at === "string") {
      expiryMs = Date.parse(promoData.expires_at);
    }

    if (expiryMs > 0 && nowMs >= expiryMs) {
      return { valid: false, reason: "Promo code has expired." };
    }
  }

  if (typeof promoData.grant_days !== "number" || promoData.grant_days <= 0) {
    return { valid: false, reason: "Promo code is missing valid 'grant_days' field." };
  }

  const maxRedemptions = typeof promoData.max_redemptions === "number" ? promoData.max_redemptions : 1;
  const redeemedCount = typeof promoData.redeemed_count === "number" ? promoData.redeemed_count : 0;

  if (redeemedCount >= maxRedemptions) {
    return { valid: false, reason: "Promo code has reached maximum redemptions." };
  }

  if (redemptionDocExists) {
    return { valid: false, reason: "Promo code has already been redeemed by this shop." };
  }

  return {
    valid: true,
    grantDays: promoData.grant_days,
    maxRedemptions,
    redeemedCount,
  };
}

module.exports = {
  mapSubscriptionState,
  isOwnerAuthorized,
  validateTokenBinding,
  validatePromoCode,
};
