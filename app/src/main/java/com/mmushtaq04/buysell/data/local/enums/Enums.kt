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
    PARTNER
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
    OTHER
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
