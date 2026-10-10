package com.mmushtaq04.buysell.domain.model

enum class Scope {
    PUBLIC,
    VAULT
}

enum class Role {
    OWNER,
    PARTNER,
    STAFF
}

enum class PaymentDirection {
    IN,  // Wasooli / Customer payment received
    OUT  // Payment given to supplier
}

enum class TxnType {
    PURCHASE,
    SALE,
    PURCHASE_RETURN,
    SALE_RETURN
}

enum class SyncOp {
    UPSERT,
    DELETE
}

enum class PaymentMethod {
    CASH,
    BANK,
    ONLINE
}

fun standalonePaymentScope(direction: PaymentDirection): Scope {
    return when (direction) {
        PaymentDirection.IN -> Scope.PUBLIC
        PaymentDirection.OUT -> Scope.VAULT
    }
}
