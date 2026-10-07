package com.mmushtaq04.buysell.data.local.entity

import androidx.room.*
import com.mmushtaq04.buysell.data.local.enums.*

@Entity(
    tableName = "shops"
)
data class ShopEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "receipt_prefix") val receiptSeqPrefix: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "users"
)
data class UserEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "email") val email: String? = null,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "photo_url") val photoUrl: String? = null,
    @ColumnInfo(name = "role") val role: Role = Role.STAFF,
    @ColumnInfo(name = "active_session_id") val activeSessionId: String? = null,
    @ColumnInfo(name = "active_device_id") val activeDeviceId: String? = null,
    @ColumnInfo(name = "session_updated_at") val sessionUpdatedAt: Long? = null,

    // Common columns
    @ColumnInfo(name = "shop_id") val shopId: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "shop_members",
    indices = [Index("shop_id"), Index("user_id")]
)
data class ShopMemberEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "role") val role: Role,
    @ColumnInfo(name = "status") val status: MemberStatus,
    @ColumnInfo(name = "invited_by") val invitedBy: String? = null,
    @ColumnInfo(name = "joined_at") val joinedAt: Long? = null,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "invites",
    indices = [Index("shop_id")]
)
data class InviteEntity(
    @PrimaryKey @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "role") val role: Role,
    @ColumnInfo(name = "created_by_user_id") val createdByUserId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    @ColumnInfo(name = "used_at") val usedAt: Long? = null,
    @ColumnInfo(name = "used_by_user_id") val usedByUserId: String? = null
)

@Entity(
    tableName = "categories",
    indices = [Index("shop_id")]
)
data class CategoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "has_unique_items") val hasUniqueItems: Boolean = true,
    @ColumnInfo(name = "requires_imei") val requiresImei: Boolean = true,
    @ColumnInfo(name = "attributes_schema_json") val attributesSchemaJson: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "parties",
    indices = [Index("shop_id")]
)
data class PartyEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "cnic") val cnic: String? = null,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "type_hint") val typeHint: PartyTypeHint = PartyTypeHint.BOTH,
    @ColumnInfo(name = "id_photo_uri") val idPhotoUri: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "stock_items",
    indices = [
        Index("shop_id"),
        Index("category_id"),
        Index("identifier"),
        Index("identifier2"),
        Index("purchase_line_id")
    ]
)
data class StockItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "brand") val brand: String,
    @ColumnInfo(name = "model") val model: String,
    @ColumnInfo(name = "identifier") val identifier: String? = null, // IMEI1 or Serial
    @ColumnInfo(name = "identifier2") val identifier2: String? = null, // IMEI2
    @ColumnInfo(name = "attributes") val attributes: String? = null, // JSON
    @ColumnInfo(name = "condition") val condition: String = "GOOD",
    @ColumnInfo(name = "quantity") val quantity: Int = 1,
    @ColumnInfo(name = "remaining_qty") val remainingQty: Int = 1,
    @ColumnInfo(name = "status") val status: ItemStatus = ItemStatus.IN_STOCK,
    @ColumnInfo(name = "purchase_line_id") val purchaseLineId: String? = null,
    @ColumnInfo(name = "stocked_at") val stockedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "has_conflict") val hasConflict: Boolean = false,
    @ColumnInfo(name = "notes") val notes: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "txns",
    indices = [
        Index("shop_id"),
        Index("party_id"),
        Index("exchange_group_id"),
        Index("original_txn_id")
    ]
)
data class TxnEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "type") val type: TxnType,
    @ColumnInfo(name = "party_id") val partyId: String? = null,
    @ColumnInfo(name = "txn_date") val txnDate: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Long, // minor units (e.g. PKR paisa / cents)
    @ColumnInfo(name = "receipt_no") val receiptNo: String? = null,
    @ColumnInfo(name = "exchange_group_id") val exchangeGroupId: String? = null,
    @ColumnInfo(name = "original_txn_id") val originalTxnId: String? = null,
    @ColumnInfo(name = "scope") val scope: Scope = Scope.PUBLIC,
    @ColumnInfo(name = "note") val note: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "txn_lines",
    indices = [Index("shop_id"), Index("txn_id"), Index("stock_item_id")]
)
data class TxnLineEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "txn_id") val txnId: String,
    @ColumnInfo(name = "stock_item_id") val stockItemId: String,
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "unit_price") val unitPrice: Long, // minor units
    @ColumnInfo(name = "line_total") val lineTotal: Long, // minor units
    @ColumnInfo(name = "scope") val scope: Scope = Scope.PUBLIC,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "payments",
    indices = [Index("shop_id"), Index("txn_id"), Index("party_id"), Index("reverses_payment_id")]
)
data class PaymentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "txn_id") val txnId: String? = null,
    @ColumnInfo(name = "party_id") val partyId: String,
    @ColumnInfo(name = "direction") val direction: PaymentDirection,
    @ColumnInfo(name = "amount") val amount: Long, // minor units
    @ColumnInfo(name = "method") val method: PaymentMethod,
    @ColumnInfo(name = "account_id") val accountId: String? = null,
    @ColumnInfo(name = "reference_no") val referenceNo: String? = null,
    @ColumnInfo(name = "counterparty_info") val counterpartyInfo: String? = null,
    @ColumnInfo(name = "pay_date") val payDate: Long,
    @ColumnInfo(name = "reverses_payment_id") val reversesPaymentId: String? = null,
    @ColumnInfo(name = "scope") val scope: Scope,
    @ColumnInfo(name = "note") val note: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "payment_accounts",
    indices = [Index("shop_id")]
)
data class PaymentAccountEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "type") val type: PaymentAccountType,
    @ColumnInfo(name = "details") val details: String? = null,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "active") val active: Boolean = true,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "payment_promises",
    indices = [
        Index("shop_id"),
        Index("party_id"),
        Index("txn_id"),
        Index("status"),
        Index("promised_date")
    ]
)
data class PaymentPromiseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "party_id") val partyId: String,
    @ColumnInfo(name = "txn_id") val txnId: String? = null,
    @ColumnInfo(name = "direction") val direction: PromiseDirection,
    @ColumnInfo(name = "amount") val amount: Long, // minor units
    @ColumnInfo(name = "promised_date") val promisedDate: Long,
    @ColumnInfo(name = "status") val status: PromiseStatus = PromiseStatus.OPEN,
    @ColumnInfo(name = "previous_promise_id") val previousPromiseId: String? = null,
    @ColumnInfo(name = "scope") val scope: Scope,
    @ColumnInfo(name = "note") val note: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "attachments",
    indices = [Index("shop_id"), Index("owner_type"), Index("owner_id")]
)
data class AttachmentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "owner_type") val ownerType: String, // e.g. "TXN", "PARTY"
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "file_uri") val fileUri: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "scope") val scope: Scope = Scope.PUBLIC,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "expenses",
    indices = [Index("shop_id"), Index("category_id")]
)
data class ExpenseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "amount") val amount: Long, // minor units
    @ColumnInfo(name = "expense_date") val expenseDate: Long,
    @ColumnInfo(name = "note") val note: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "audit_logs",
    indices = [Index("shop_id"), Index("user_id"), Index("entity_type")]
)
data class AuditLogEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "action") val action: String, // e.g. "CREATE_TXN", "UPDATE_PRICE"
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "details_json") val detailsJson: String? = null,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "conflicts",
    indices = [Index("shop_id"), Index("stock_item_id")]
)
data class ConflictEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "stock_item_id") val stockItemId: String,
    @ColumnInfo(name = "txn_id_a") val txnIdA: String,
    @ColumnInfo(name = "txn_id_b") val txnIdB: String,
    @ColumnInfo(name = "status") val status: ConflictStatus = ConflictStatus.OPEN,
    @ColumnInfo(name = "resolved_by") val resolvedBy: String? = null,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long? = null,
    @ColumnInfo(name = "resolution_note") val resolutionNote: String? = null
)

@Entity(
    tableName = "app_meta"
)
data class AppMetaEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = 1,
    @ColumnInfo(name = "device_code") val deviceCode: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "active_shop_id") val activeShopId: String? = null,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int = 1,
    @ColumnInfo(name = "receipt_seq") val receiptSeq: Int = 1
)

@Entity(
    tableName = "sync_outbox"
)
data class SyncOutboxEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "op") val op: SyncOp,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "sync_cursor"
)
data class SyncCursorEntity(
    @PrimaryKey @ColumnInfo(name = "collection_path") val collectionPath: String,
    @ColumnInfo(name = "last_pulled_at") val lastPulledAt: Long
)
