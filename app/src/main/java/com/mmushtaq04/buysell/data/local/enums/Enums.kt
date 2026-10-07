package com.mmushtaq04.buysell.data.local.enums

enum class SyncState {
    PENDING,
    SYNCED,
    CONFLICT
}

enum class IdentifierType {
    IMEI,
    SERIAL,
    NONE
}

enum class TrackingMode {
    UNIQUE,
    QUANTITY
}

enum class Role {
    OWNER,
    STAFF,
    PARTNER;

    fun toDisplayName(): String = when (this) {
        OWNER -> "Owner"
        STAFF -> "Staff"
        PARTNER -> "Sleeping Partner"
    }

    companion object {
        fun fromStr(str: String): Role = when (str.uppercase().trim()) {
            "STAFF" -> STAFF
            "PARTNER", "SLEEPING PARTNER" -> PARTNER
            else -> OWNER
        }
    }
}

enum class MemberStatus {
    INVITED,
    ACTIVE,
    REMOVED
}

enum class PartyTypeHint {
    CUSTOMER,
    SUPPLIER,
    BOTH
}

enum class ItemStatus {
    IN_STOCK,
    SOLD,
    RETURNED_TO_SUPPLIER,
    WRITTEN_OFF
}

enum class TxnType {
    PURCHASE,
    SALE,
    SALE_RETURN,
    PURCHASE_RETURN
}

enum class Scope {
    PUBLIC,
    VAULT
}

enum class PaymentDirection {
    IN,
    OUT
}

enum class PaymentMethod {
    CASH,
    BANK,
    WALLET,
    CHEQUE,
    EXCHANGE,
    OTHER;

    fun toDisplayName(): String = when (this) {
        CASH -> "Cash"
        BANK -> "Bank Transfer"
        WALLET -> "Easypaisa / JazzCash"
        CHEQUE -> "Cheque"
        EXCHANGE -> "Exchange"
        OTHER -> "Other"
    }

    companion object {
        fun fromStr(str: String): PaymentMethod = when (str.uppercase().trim()) {
            "CASH" -> CASH
            "BANK", "BANK TRANSFER" -> BANK
            "WALLET", "EASYPAISA", "JAZZCASH" -> WALLET
            "CHEQUE" -> CHEQUE
            "EXCHANGE" -> EXCHANGE
            else -> OTHER
        }
    }
}

enum class PaymentAccountType {
    CASH,
    BANK,
    WALLET
}

enum class PromiseDirection {
    RECEIVE,
    PAY
}

enum class PromiseStatus {
    OPEN,
    KEPT,
    RESCHEDULED,
    CANCELLED
}

enum class UploadState {
    LOCAL_ONLY,
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED
}

enum class ExpenseCategory {
    RENT,
    ELECTRICITY,
    SALARY,
    REPAIR,
    ACCESSORIES,
    COMMISSION,
    OTHER
}

enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    REVERSE
}

enum class ConflictStatus {
    OPEN,
    RESOLVED
}

enum class SyncOp {
    UPSERT,
    DELETE
}
