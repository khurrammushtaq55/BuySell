const { test, describe } = require("node:test");
const assert = require("node:assert/strict");
const {
  mapSubscriptionState,
  isOwnerAuthorized,
  validateTokenBinding,
  validatePromoCode,
} = require("../lib/licenseLogic");

describe("isOwnerAuthorized", () => {
  test("returns true when callerUid matches shop owner_user_id", () => {
    assert.equal(isOwnerAuthorized("user_123", "user_123"), true);
  });

  test("returns false when callerUid does not match shop owner_user_id", () => {
    assert.equal(isOwnerAuthorized("user_456", "user_123"), false);
  });

  test("returns false when either parameter is null or empty", () => {
    assert.equal(isOwnerAuthorized(null, "user_123"), false);
    assert.equal(isOwnerAuthorized("user_123", null), false);
    assert.equal(isOwnerAuthorized("", ""), false);
  });
});

describe("validateTokenBinding", () => {
  test("allows token binding when token does not exist in purchases", () => {
    const result = validateTokenBinding(null, "shop_A", null);
    assert.equal(result.valid, true);
  });

  test("allows token binding when token belongs to same shop", () => {
    const existing = { shop_id: "shop_A" };
    const result = validateTokenBinding(existing, "shop_A", null);
    assert.equal(result.valid, true);
  });

  test("rejects token binding when token belongs to a different shop", () => {
    const existing = { shop_id: "shop_B" };
    const result = validateTokenBinding(existing, "shop_A", null);
    assert.equal(result.valid, false);
    assert.match(result.reason, /already bound/);
  });

  test("validates obfuscatedExternalAccountId matching target shopId", () => {
    const extAccounts = { obfuscatedExternalAccountId: "shop_A" };
    const result = validateTokenBinding(null, "shop_A", extAccounts);
    assert.equal(result.valid, true);
  });

  test("rejects when obfuscatedExternalAccountId differs from target shopId", () => {
    const extAccounts = { obfuscatedExternalAccountId: "shop_X" };
    const result = validateTokenBinding(null, "shop_A", extAccounts);
    assert.equal(result.valid, false);
    assert.match(result.reason, /does not match/);
  });
});

describe("mapSubscriptionState", () => {
  const futureIso = new Date(Date.now() + 86400000 * 30).toISOString();
  const pastIso = new Date(Date.now() - 86400000).toISOString();

  test("maps SUBSCRIPTION_STATE_ACTIVE correctly", () => {
    const subV2 = {
      subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
      lineItems: [
        {
          productId: "pro_annual",
          expiryTime: futureIso,
          autoRenewingPlan: { autoRenewEnabled: true },
        },
      ],
    };
    const result = mapSubscriptionState(subV2, "pro_annual");
    assert.equal(result.status, "ACTIVE");
    assert.equal(result.autoRenewing, true);
    assert.equal(result.productId, "pro_annual");
    assert.equal(result.validUntilMs, Date.parse(futureIso));
  });

  test("maps SUBSCRIPTION_STATE_IN_GRACE_PERIOD correctly", () => {
    const subV2 = {
      subscriptionState: "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
      lineItems: [{ productId: "pro_monthly", expiryTime: futureIso }],
    };
    const result = mapSubscriptionState(subV2, "pro_monthly");
    assert.equal(result.status, "GRACE");
  });

  test("maps SUBSCRIPTION_STATE_ON_HOLD and SUBSCRIPTION_STATE_PAUSED correctly", () => {
    const subV2Hold = {
      subscriptionState: "SUBSCRIPTION_STATE_ON_HOLD",
      lineItems: [{ expiryTime: futureIso }],
    };
    assert.equal(mapSubscriptionState(subV2Hold).status, "ON_HOLD");

    const subV2Paused = {
      subscriptionState: "SUBSCRIPTION_STATE_PAUSED",
      lineItems: [{ expiryTime: futureIso }],
    };
    assert.equal(mapSubscriptionState(subV2Paused).status, "ON_HOLD");
  });

  test("maps SUBSCRIPTION_STATE_CANCELED to CANCELED_ACTIVE if not yet expired", () => {
    const subV2 = {
      subscriptionState: "SUBSCRIPTION_STATE_CANCELED",
      lineItems: [
        {
          expiryTime: futureIso,
          autoRenewingPlan: { autoRenewEnabled: false },
        },
      ],
    };
    const result = mapSubscriptionState(subV2, "pro_monthly");
    assert.equal(result.status, "CANCELED_ACTIVE");
    assert.equal(result.autoRenewing, false);
  });

  test("maps SUBSCRIPTION_STATE_CANCELED to EXPIRED if past expiry", () => {
    const subV2 = {
      subscriptionState: "SUBSCRIPTION_STATE_CANCELED",
      lineItems: [{ expiryTime: pastIso }],
    };
    const result = mapSubscriptionState(subV2, "pro_monthly");
    assert.equal(result.status, "EXPIRED");
  });

  test("maps null or empty response to EXPIRED", () => {
    assert.equal(mapSubscriptionState(null).status, "EXPIRED");
  });
});

describe("validatePromoCode", () => {
  const nowMs = 1700000000000;

  test("rejects missing promo code", () => {
    const result = validatePromoCode(null, false, nowMs);
    assert.equal(result.valid, false);
    assert.match(result.reason, /does not exist/);
  });

  test("rejects inactive promo code", () => {
    const promo = { active: false, grant_days: 30 };
    const result = validatePromoCode(promo, false, nowMs);
    assert.equal(result.valid, false);
    assert.match(result.reason, /inactive/);
  });

  test("rejects expired promo code", () => {
    const promo = { active: true, grant_days: 30, expires_at: nowMs - 1000 };
    const result = validatePromoCode(promo, false, nowMs);
    assert.equal(result.valid, false);
    assert.match(result.reason, /expired/);
  });

  test("rejects missing or non-positive grant_days", () => {
    const promoMissing = { active: true };
    const promoZero = { active: true, grant_days: 0 };
    assert.equal(validatePromoCode(promoMissing, false, nowMs).valid, false);
    assert.equal(validatePromoCode(promoZero, false, nowMs).valid, false);
  });

  test("rejects when maximum redemptions reached", () => {
    const promo = { active: true, grant_days: 30, max_redemptions: 5, redeemed_count: 5 };
    const result = validatePromoCode(promo, false, nowMs);
    assert.equal(result.valid, false);
    assert.match(result.reason, /maximum redemptions/);
  });

  test("defaults max_redemptions to 1 if missing", () => {
    const promo = { active: true, grant_days: 30, redeemed_count: 1 };
    const result = validatePromoCode(promo, false, nowMs);
    assert.equal(result.valid, false);
    assert.match(result.reason, /maximum redemptions/);
  });

  test("rejects if already redeemed by this shop", () => {
    const promo = { active: true, grant_days: 30, max_redemptions: 10, redeemed_count: 1 };
    const result = validatePromoCode(promo, true, nowMs);
    assert.equal(result.valid, false);
    assert.match(result.reason, /already been redeemed/);
  });

  test("approves valid promo code", () => {
    const promo = { active: true, grant_days: 30, max_redemptions: 10, redeemed_count: 2 };
    const result = validatePromoCode(promo, false, nowMs);
    assert.equal(result.valid, true);
    assert.equal(result.grantDays, 30);
  });
});
