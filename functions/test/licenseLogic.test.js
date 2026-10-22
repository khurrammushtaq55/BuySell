const { test, describe } = require("node:test");
const assert = require("node:assert/strict");
const {
  hashToken,
  shouldUpdateLicense,
  computePromoExtension,
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
  const validExtAccounts = { obfuscatedExternalAccountId: "shop_A" };

  test("allows token binding when obfuscatedExternalAccountId matches target shopId", () => {
    const result = validateTokenBinding(null, "shop_A", validExtAccounts);
    assert.equal(result.valid, true);
  });

  test("allows token binding when token belongs to same shop and obfuscated ID matches", () => {
    const existing = { shop_id: "shop_A" };
    const result = validateTokenBinding(existing, "shop_A", validExtAccounts);
    assert.equal(result.valid, true);
  });

  test("rejects token binding when token belongs to a different shop in purchases collection", () => {
    const existing = { shop_id: "shop_B" };
    const result = validateTokenBinding(existing, "shop_A", validExtAccounts);
    assert.equal(result.valid, false);
    assert.match(result.reason, /already bound/);
  });

  test("rejects token binding when externalAccountIdentifiers or obfuscatedExternalAccountId is missing", () => {
    assert.equal(validateTokenBinding(null, "shop_A", null).valid, false);
    assert.equal(validateTokenBinding(null, "shop_A", {}).valid, false);
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

describe("hashToken and shouldUpdateLicense", () => {
  const nowMs = 1700000000000;
  const tokenA = "token_monthly_123";
  const tokenB = "token_annual_456";
  const hashA = hashToken(tokenA);
  const hashB = hashToken(tokenB);

  test("hashToken returns consistent non-empty SHA-256 string", () => {
    assert.equal(typeof hashA, "string");
    assert.equal(hashA.length, 64);
    assert.equal(hashToken(tokenA), hashA);
  });

  test("allows update when no current license exists", () => {
    assert.equal(shouldUpdateLicense(null, hashA, nowMs + 100000, nowMs), true);
  });

  test("allows update when current license is expired as of nowMs", () => {
    const current = { status: "EXPIRED", valid_until: nowMs - 1000, owning_token_hash: hashA };
    assert.equal(shouldUpdateLicense(current, hashB, nowMs + 50000, nowMs), true);
  });

  test("allows update when incoming token matches current owning_token_hash (renewal/cancellation)", () => {
    const current = { status: "ACTIVE", valid_until: nowMs + 500000, owning_token_hash: hashA };
    // Renewal for same token A with new validUntil
    assert.equal(shouldUpdateLicense(current, hashA, nowMs + 1000000, nowMs), true);
    // Cancellation for same token A with earlier or same validUntil
    assert.equal(shouldUpdateLicense(current, hashA, nowMs + 200000, nowMs), true);
  });

  test("allows update when incoming token grants LATER expiration date than current (upgrade)", () => {
    const current = { status: "ACTIVE", valid_until: nowMs + 500000, owning_token_hash: hashA };
    const incomingValidUntil = nowMs + 1000000; // Annual upgrade
    assert.equal(shouldUpdateLicense(current, hashB, incomingValidUntil, nowMs), true);
  });

  test("rejects update when an old/expired token attempts to downgrade an active newer subscription", () => {
    const current = { status: "ACTIVE", valid_until: nowMs + 1000000, owning_token_hash: hashB }; // Active Annual
    const oldMonthlyValidUntil = nowMs + 200000; // Late RTDN for old Monthly
    assert.equal(shouldUpdateLicense(current, hashA, oldMonthlyValidUntil, nowMs), false);
  });

  test("rejects sabotage attempt by attacker token against active victim license", () => {
    const current = { status: "ACTIVE", valid_until: nowMs + 500000, owning_token_hash: hashA };
    const attackerHash = hashToken("attacker_token_999");
    assert.equal(shouldUpdateLicense(current, attackerHash, 0, nowMs), false);
  });
});

describe("computePromoExtension", () => {
  const nowMs = 1700000000000;
  const dayMs = 86400000;

  test("stacks promo days on top of an active paid PLAY subscription without losing PLAY attributes", () => {
    const currentPlayLicense = {
      plan: "buysell_pro_annual",
      status: "ACTIVE",
      valid_until: nowMs + 300 * dayMs,
      source: "PLAY",
      owning_token_hash: "hash_annual_token",
      auto_renewing: true,
    };

    const res = computePromoExtension(currentPlayLicense, "VIP30", "shop_1", 30, nowMs);

    assert.equal(res.status, "ACTIVE");
    assert.equal(res.source, "PLAY");
    assert.equal(res.plan, "buysell_pro_annual");
    assert.equal(res.owningTokenHash, "hash_annual_token");
    assert.equal(res.autoRenewing, true);
    assert.equal(res.validUntilMs, nowMs + 330 * dayMs); // 300 + 30 days = 330 days!
  });

  test("stacks promo days on top of an active promo license", () => {
    const currentPromoLicense = {
      plan: "VIP30",
      status: "ACTIVE",
      valid_until: nowMs + 10 * dayMs,
      source: "PROMO",
    };

    const res = computePromoExtension(currentPromoLicense, "BONUS15", "shop_1", 15, nowMs);

    assert.equal(res.status, "ACTIVE");
    assert.equal(res.source, "PROMO");
    assert.equal(res.plan, "BONUS15");
    assert.equal(res.validUntilMs, nowMs + 25 * dayMs); // 10 + 15 days = 25 days!
  });

  test("grants promo days from nowMs when current license is expired", () => {
    const expiredLicense = {
      plan: "buysell_pro_monthly",
      status: "EXPIRED",
      valid_until: nowMs - 5 * dayMs,
      source: "PLAY",
    };

    const res = computePromoExtension(expiredLicense, "VIP30", "shop_1", 30, nowMs);

    assert.equal(res.status, "ACTIVE");
    assert.equal(res.source, "PROMO");
    assert.equal(res.plan, "VIP30");
    assert.equal(res.validUntilMs, nowMs + 30 * dayMs);
  });
});

