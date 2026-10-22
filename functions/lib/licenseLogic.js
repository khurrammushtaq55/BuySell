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

  if (!externalAccountIdentifiers || !externalAccountIdentifiers.obfuscatedExternalAccountId) {
    return {
      valid: false,
      reason: "Purchase token is missing obfuscated external account ID.",
    };
  }

  const obfId = externalAccountIdentifiers.obfuscatedExternalAccountId;
  if (obfId !== targetShopId) {
    return {
      valid: false,
      reason: `Obfuscated external account ID '${obfId}' does not match target shop '${targetShopId}'`,
    };
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

const crypto = require("crypto");

/**
 * Computes a deterministic SHA-256 hash of a purchase token (or promo source).
 * @param {string} token
 * @returns {string}
 */
function hashToken(token) {
  if (!token) return "";
  return crypto.createHash("sha256").update(String(token).trim()).digest("hex");
}

/**
 * Evaluates whether an incoming license update should be allowed to overwrite
 * the current license document (`shops/{shopId}/license/current`).
 *
 * @param {object|null} currentLicense - Current document data from shops/{shopId}/license/current
 * @param {string} incomingTokenHash - SHA-256 Hash of purchase token
 * @param {number} incomingValidUntilMs - Expiration timestamp of incoming license
 * @param {number} nowMs - Current time in ms
 * @returns {boolean} true if update should be accepted, false if rejected
 */
function shouldUpdateLicense(currentLicense, incomingTokenHash, incomingValidUntilMs, nowMs) {
  if (!currentLicense || !currentLicense.status) {
    return true;
  }

  const currentValidUntil = typeof currentLicense.valid_until === "number" ? currentLicense.valid_until : 0;
  const currentTokenHash = currentLicense.owning_token_hash || "";

  // If current license is already expired as of now, accept any new valid update
  if (currentValidUntil <= nowMs) {
    return true;
  }

  // Same owning token -> Always allow update (renewal, status change, cancellation)
  if (incomingTokenHash && currentTokenHash && incomingTokenHash === currentTokenHash) {
    return true;
  }

  // Different token/source -> Allow ONLY IF incoming update grants a LATER expiration date
  if (incomingValidUntilMs > currentValidUntil) {
    return true;
  }

  return false;
}

/**
 * Computes the new license fields when applying a promo code.
 * Extends the existing license if currently active, and preserves paid PLAY attributes.
 *
 * @param {object|null} currentLicense - Current document data from shops/{shopId}/license/current
 * @param {string} promoCode - Cleaned promo code string
 * @param {string} shopId - Target shop ID
 * @param {number} grantDays - Number of days to grant
 * @param {number} nowMs - Current time in ms
 * @returns {object} { plan, status, validUntilMs, source, owningTokenHash, autoRenewing }
 */
function computePromoExtension(currentLicense, promoCode, shopId, grantDays, nowMs) {
  const currentValidUntil = (currentLicense && typeof currentLicense.valid_until === "number")
    ? currentLicense.valid_until
    : 0;

  const isActiveInFuture = currentLicense && currentValidUntil > nowMs && currentLicense.status !== "EXPIRED";
  const baseMs = isActiveInFuture ? currentValidUntil : nowMs;
  const grantMs = grantDays * 24 * 60 * 60 * 1000;
  const validUntilMs = baseMs + grantMs;

  const isCurrentPlayActive = currentLicense &&
    currentLicense.source === "PLAY" &&
    isActiveInFuture;

  const promoTokenHash = hashToken(`PROMO_${promoCode}_${shopId}`);

  return {
    plan: isCurrentPlayActive ? (currentLicense.plan || promoCode) : promoCode,
    status: "ACTIVE",
    validUntilMs,
    source: isCurrentPlayActive ? "PLAY" : "PROMO",
    owningTokenHash: isCurrentPlayActive
      ? (currentLicense.owning_token_hash || promoTokenHash)
      : promoTokenHash,
    autoRenewing: isCurrentPlayActive ? Boolean(currentLicense.auto_renewing) : false,
  };
}

module.exports = {
  hashToken,
  shouldUpdateLicense,
  computePromoExtension,
  mapSubscriptionState,
  isOwnerAuthorized,
  validateTokenBinding,
  validatePromoCode,
};
