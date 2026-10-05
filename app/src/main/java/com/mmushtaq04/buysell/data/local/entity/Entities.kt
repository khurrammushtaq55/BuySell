package com.mmushtaq04.buysell.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mmushtaq04.buysell.data.local.enums.*

@Entity(
    tableName = "shops",
    indices = [Index("owner_user_id")]
)
data class ShopEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone") val phone: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "currency") val currency: String = "PKR",
    @ColumnInfo(name = "timezone") val timezone: String = "Asia/Karachi",
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "plan") val plan: String = "free",
    @ColumnInfo(name = "receipt_footer") val receiptFooter: String? = null,
    @ColumnInfo(name = "logo_uri") val logoUri: String? = null,
    @ColumnInfo(name = "slow_stock_days") val slowStockDays: Int = 30,
    @ColumnInfo(name = "preferred_language") val preferredLanguage: String? = null,
    @ColumnInfo(name = "cloud_photo_upload_enabled") val cloudPhotoUploadEnabled: Boolean = false,

    // Common columns
    @ColumnInfo(name = "shop_id") val shopId: String,
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
    indices = [Index(value = ["shop_id", "user_id"], unique = true)]
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
    @PrimaryKey @ColumnInfo(name = "code") val code: String, // 8 chars PK
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "role") val role: Role,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    @ColumnInfo(name = "used_by") val usedBy: String? = null,
    @ColumnInfo(name = "used_at") val usedAt: Long? = null,

    // Common columns
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.PENDING
)

@Entity(
    tableName = "categories",
    indices = [Index("shop_id")]
)
data class CategoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "preset_key") val presetKey: String,
    @ColumnInfo(name = "identifier_type") val identifierType: IdentifierType,
    @ColumnInfo(name = "tracking_mode") val trackingMode: TrackingMode,
    @ColumnInfo(name = "field_schema") val fieldSchema: String? = null, // JSON
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,

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
    indices = [Index("shop_id"), Index("phone"), Index("cnic")]
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
        Index(value = ["shop_id", "status"]),
        Index(value = ["shop_id", "identifier"]),
        Index(value = ["shop_id", "stocked_at"]),
        Index(value = ["shop_id", "category_id", "brand", "model", "status"])
    ]
)
data class StockItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "brand") val brand: String,
    @ColumnInfo(name = "model") val model: String,
    @ColumnInfo(name = "identifier") val identifier: String? = null,
    @ColumnInfo(name = "identifier2") val identifier2: String? = null,
    @ColumnInfo(name = "attributes") val attributes: String? = null, // JSON
    @ColumnInfo(name = "condition") val condition: String? = null,
    @ColumnInfo(name = "quantity") val quantity: Int = 1,
    @ColumnInfo(name = "remaining_qty") val remainingQty: Int = 1,
    @ColumnInfo(name = "status") val status: ItemStatus = ItemStatus.IN_STOCK,
    @ColumnInfo(name = "purchase_line_id") val purchaseLineId: String? = null,
    @ColumnInfo(name = "stocked_at") val stockedAt: Long,
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
    @ColumnInfo(name = "party_id") val partyId: String,
    @ColumnInfo(name = "txn_date") val txnDate: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Long, // minor units (PKR paisa)
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
    indices = [Index("txn_id"), Index("stock_item_id")]
)
data class TxnLineEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "txn_id") val txnId: String,
    @ColumnInfo(name = "stock_item_id") val stockItemId: String,
    @ColumnInfo(name = "quantity") val quantity: Int = 1,
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
    indices = [Index("shop_id"), Index("party_id"), Index("txn_id"), Index("promised_date")]
)
data class PaymentPromiseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "party_id") val partyId: String,
    @ColumnInfo(name = "txn_id") val txnId: String? = null,
    @ColumnInfo(name = "direction") val direction: PromiseDirection,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "promised_date") val promisedDate: Long,
    @ColumnInfo(name = "status") val status: PromiseStatus = PromiseStatus.OPEN,
    @ColumnInfo(name = "previous_promise_id") val previousPromiseId: String? = null,
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
    tableName = "attachments",
    indices = [Index("shop_id"), Index(value = ["entity_type", "entity_id"])]
)
data class AttachmentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "local_path") val localPath: String? = null,
    @ColumnInfo(name = "remote_path") val remotePath: String? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long? = null,
    @ColumnInfo(name = "width") val width: Int? = null,
    @ColumnInfo(name = "height") val height: Int? = null,
    @ColumnInfo(name = "upload_state") val uploadState: UploadState = UploadState.LOCAL_ONLY,
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
    indices = [Index("shop_id"), Index("stock_item_id"), Index("expense_date")]
)
data class ExpenseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "stock_item_id") val stockItemId: String? = null,
    @ColumnInfo(name = "category") val category: ExpenseCategory,
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
    tableName = "audit_log",
    indices = [Index("shop_id"), Index("entity_id")]
)
data class AuditLogEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "shop_id") val shopId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "action") val action: AuditAction,
    @ColumnInfo(name = "before_json") val beforeJson: String? = null,
    @ColumnInfo(name = "after_json") val afterJson: String? = null,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "at") val at: Long,
    @ColumnInfo(name = "scope") val scope: Scope = Scope.PUBLIC
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
    @ColumnInfo(name = "receipt_seq") val receiptSeq: Int = 1,
    @ColumnInfo(name = "active_shop_id") val activeShopId: String? = null,
    @ColumnInfo(name = "app_pin_hash") val appPinHash: String? = null
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
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "attempts") val attempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "session_id_at_enqueue") val sessionIdAtEnqueue: String? = null
)

@Entity(
    tableName = "sync_cursor"
)
data class SyncCursorEntity(
    @PrimaryKey @ColumnInfo(name = "collection_path") val collectionPath: String,
    @ColumnInfo(name = "last_pulled_at") val lastPulledAt: Long,
    @ColumnInfo(name = "last_rev_seen") val lastRevSeen: Long? = null
)
