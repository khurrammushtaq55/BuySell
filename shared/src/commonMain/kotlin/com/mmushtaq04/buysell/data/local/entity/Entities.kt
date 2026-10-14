package com.mmushtaq04.buysell.data.local.entity

import androidx.room.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mmushtaq04.buysell.data.local.enums.*

@Serializable
@Entity(
    tableName = "shops"
)
data class ShopEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "name") @SerialName("name") val name: String,
    @ColumnInfo(name = "code") @SerialName("code") val code: String,
    @ColumnInfo(name = "owner_user_id") @SerialName("owner_user_id") val ownerUserId: String,
    @ColumnInfo(name = "phone") @SerialName("phone") val phone: String? = null,
    @ColumnInfo(name = "address") @SerialName("address") val address: String? = null,
    @ColumnInfo(name = "receipt_prefix") @SerialName("receipt_prefix") val receiptSeqPrefix: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
@Entity(
    tableName = "users"
)
data class UserEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "display_name") @SerialName("display_name") val displayName: String,
    @ColumnInfo(name = "email") @SerialName("email") val email: String? = null,
    @ColumnInfo(name = "phone") @SerialName("phone") val phone: String? = null,
    @ColumnInfo(name = "photo_url") @SerialName("photo_url") val photoUrl: String? = null,
    @ColumnInfo(name = "role") @SerialName("role") val role: Role = Role.STAFF,
    @ColumnInfo(name = "active_session_id") @SerialName("active_session_id") val activeSessionId: String? = null,
    @ColumnInfo(name = "active_device_id") @SerialName("active_device_id") val activeDeviceId: String? = null,
    @ColumnInfo(name = "session_updated_at") @SerialName("session_updated_at") val sessionUpdatedAt: Long? = null,

    // Common columns
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String = "",
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
@Entity(
    tableName = "shop_members",
    indices = [Index("shop_id"), Index("user_id")]
)
data class ShopMemberEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "user_id") @SerialName("user_id") val userId: String,
    @ColumnInfo(name = "role") @SerialName("role") val role: Role,
    @ColumnInfo(name = "status") @SerialName("status") val status: MemberStatus,
    @ColumnInfo(name = "invited_by") @SerialName("invited_by") val invitedBy: String? = null,
    @ColumnInfo(name = "joined_at") @SerialName("joined_at") val joinedAt: Long? = null,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
@Entity(
    tableName = "invites",
    indices = [Index("shop_id")]
)
data class InviteEntity(
    @PrimaryKey @ColumnInfo(name = "code") @SerialName("code") val code: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "role") @SerialName("role") val role: Role,
    @ColumnInfo(name = "created_by_user_id") @SerialName("created_by_user_id") val createdByUserId: String,
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "expires_at") @SerialName("expires_at") val expiresAt: Long,
    @ColumnInfo(name = "used_at") @SerialName("used_at") val usedAt: Long? = null,
    @ColumnInfo(name = "used_by_user_id") @SerialName("used_by_user_id") val usedByUserId: String? = null
)

@Serializable
@Entity(
    tableName = "categories",
    indices = [Index("shop_id")]
)
data class CategoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "name") @SerialName("name") val name: String,
    @ColumnInfo(name = "code") @SerialName("code") val code: String,
    @ColumnInfo(name = "has_unique_items") @SerialName("has_unique_items") val hasUniqueItems: Boolean = true,
    @ColumnInfo(name = "requires_imei") @SerialName("requires_imei") val requiresImei: Boolean = true,
    @ColumnInfo(name = "attributes_schema_json") @SerialName("attributes_schema_json") val attributesSchemaJson: String? = null,
    @ColumnInfo(name = "sort_order") @SerialName("sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "enabled") @SerialName("enabled") val enabled: Boolean = true,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
@Entity(
    tableName = "parties",
    indices = [Index("shop_id")]
)
data class PartyEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "name") @SerialName("name") val name: String,
    @ColumnInfo(name = "phone") @SerialName("phone") val phone: String? = null,
    @ColumnInfo(name = "cnic") @SerialName("cnic") val cnic: String? = null,
    @ColumnInfo(name = "address") @SerialName("address") val address: String? = null,
    @ColumnInfo(name = "notes") @SerialName("notes") val notes: String? = null,
    @ColumnInfo(name = "type_hint") @SerialName("type_hint") val typeHint: PartyTypeHint = PartyTypeHint.BOTH,
    @ColumnInfo(name = "id_photo_uri") @SerialName("id_photo_uri") val idPhotoUri: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
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
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "category_id") @SerialName("category_id") val categoryId: String,
    @ColumnInfo(name = "brand") @SerialName("brand") val brand: String,
    @ColumnInfo(name = "model") @SerialName("model") val model: String,
    @ColumnInfo(name = "identifier") @SerialName("identifier") val identifier: String? = null,
    @ColumnInfo(name = "identifier2") @SerialName("identifier2") val identifier2: String? = null,
    @ColumnInfo(name = "attributes") @SerialName("attributes") val attributes: String? = null,
    @ColumnInfo(name = "condition") @SerialName("condition") val condition: String = "GOOD",
    @ColumnInfo(name = "quantity") @SerialName("quantity") val quantity: Int = 1,
    @ColumnInfo(name = "remaining_qty") @SerialName("remaining_qty") val remainingQty: Int = 1,
    @ColumnInfo(name = "status") @SerialName("status") val status: ItemStatus = ItemStatus.IN_STOCK,
    @ColumnInfo(name = "purchase_line_id") @SerialName("purchase_line_id") val purchaseLineId: String? = null,
    @ColumnInfo(name = "stocked_at") @SerialName("stocked_at") val stockedAt: Long = 0L,
    @ColumnInfo(name = "has_conflict") @SerialName("has_conflict") val hasConflict: Boolean = false,
    @ColumnInfo(name = "notes") @SerialName("notes") val notes: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
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
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "type") @SerialName("type") val type: TxnType,
    @ColumnInfo(name = "party_id") @SerialName("party_id") val partyId: String? = null,
    @ColumnInfo(name = "txn_date") @SerialName("txn_date") val txnDate: Long,
    @ColumnInfo(name = "total_amount") @SerialName("total_amount") val totalAmount: Long,
    @ColumnInfo(name = "receipt_no") @SerialName("receipt_no") val receiptNo: String? = null,
    @ColumnInfo(name = "exchange_group_id") @SerialName("exchange_group_id") val exchangeGroupId: String? = null,
    @ColumnInfo(name = "original_txn_id") @SerialName("original_txn_id") val originalTxnId: String? = null,
    @ColumnInfo(name = "scope") @SerialName("scope") val scope: Scope = Scope.PUBLIC,
    @ColumnInfo(name = "note") @SerialName("note") val note: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
@Entity(
    tableName = "txn_lines",
    indices = [Index("shop_id"), Index("txn_id"), Index("stock_item_id")]
)
data class TxnLineEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "txn_id") @SerialName("txn_id") val txnId: String,
    @ColumnInfo(name = "stock_item_id") @SerialName("stock_item_id") val stockItemId: String,
    @ColumnInfo(name = "quantity") @SerialName("quantity") val quantity: Int,
    @ColumnInfo(name = "unit_price") @SerialName("unit_price") val unitPrice: Long,
    @ColumnInfo(name = "line_total") @SerialName("line_total") val lineTotal: Long,
    @ColumnInfo(name = "scope") @SerialName("scope") val scope: Scope = Scope.PUBLIC,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
@Entity(
    tableName = "payments",
    indices = [Index("shop_id"), Index("txn_id"), Index("party_id"), Index("reverses_payment_id")]
)
data class PaymentEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "txn_id") @SerialName("txn_id") val txnId: String? = null,
    @ColumnInfo(name = "party_id") @SerialName("party_id") val partyId: String,
    @ColumnInfo(name = "direction") @SerialName("direction") val direction: PaymentDirection,
    @ColumnInfo(name = "amount") @SerialName("amount") val amount: Long,
    @ColumnInfo(name = "method") @SerialName("method") val method: PaymentMethod,
    @ColumnInfo(name = "account_id") @SerialName("account_id") val accountId: String? = null,
    @ColumnInfo(name = "reference_no") @SerialName("reference_no") val referenceNo: String? = null,
    @ColumnInfo(name = "counterparty_info") @SerialName("counterparty_info") val counterpartyInfo: String? = null,
    @ColumnInfo(name = "pay_date") @SerialName("pay_date") val payDate: Long,
    @ColumnInfo(name = "reverses_payment_id") @SerialName("reverses_payment_id") val reversesPaymentId: String? = null,
    @ColumnInfo(name = "scope") @SerialName("scope") val scope: Scope,
    @ColumnInfo(name = "note") @SerialName("note") val note: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
@Entity(
    tableName = "payment_accounts",
    indices = [Index("shop_id")]
)
data class PaymentAccountEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "label") @SerialName("label") val label: String,
    @ColumnInfo(name = "type") @SerialName("type") val type: PaymentAccountType,
    @ColumnInfo(name = "details") @SerialName("details") val details: String? = null,
    @ColumnInfo(name = "is_default") @SerialName("is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "active") @SerialName("active") val active: Boolean = true,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
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
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "party_id") @SerialName("party_id") val partyId: String,
    @ColumnInfo(name = "txn_id") @SerialName("txn_id") val txnId: String? = null,
    @ColumnInfo(name = "direction") @SerialName("direction") val direction: PromiseDirection,
    @ColumnInfo(name = "amount") @SerialName("amount") val amount: Long,
    @ColumnInfo(name = "promised_date") @SerialName("promised_date") val promisedDate: Long,
    @ColumnInfo(name = "status") @SerialName("status") val status: PromiseStatus = PromiseStatus.OPEN,
    @ColumnInfo(name = "previous_promise_id") @SerialName("previous_promise_id") val previousPromiseId: String? = null,
    @ColumnInfo(name = "scope") @SerialName("scope") val scope: Scope,
    @ColumnInfo(name = "note") @SerialName("note") val note: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
@Entity(
    tableName = "attachments",
    indices = [Index("shop_id"), Index("owner_type"), Index("owner_id")]
)
data class AttachmentEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "owner_type") @SerialName("owner_type") val ownerType: String,
    @ColumnInfo(name = "owner_id") @SerialName("owner_id") val ownerId: String,
    @ColumnInfo(name = "file_uri") @SerialName("file_uri") val fileUri: String,
    @ColumnInfo(name = "mime_type") @SerialName("mime_type") val mimeType: String,
    @ColumnInfo(name = "scope") @SerialName("scope") val scope: Scope = Scope.PUBLIC,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
@Entity(
    tableName = "expenses",
    indices = [Index("shop_id"), Index("category_id")]
)
data class ExpenseEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "category_id") @SerialName("category_id") val categoryId: String? = null,
    @ColumnInfo(name = "amount") @SerialName("amount") val amount: Long,
    @ColumnInfo(name = "expense_date") @SerialName("expense_date") val expenseDate: Long,
    @ColumnInfo(name = "note") @SerialName("note") val note: String? = null,

    // Common columns
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") @SerialName("updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") @SerialName("created_by") val createdBy: String,
    @ColumnInfo(name = "updated_by") @SerialName("updated_by") val updatedBy: String,
    @ColumnInfo(name = "deleted_at") @SerialName("deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "deleted_by") @SerialName("deleted_by") val deletedBy: String? = null,
    @ColumnInfo(name = "rev") @SerialName("rev") val rev: Long = 1L,
    @ColumnInfo(name = "origin_device_id") @SerialName("origin_device_id") val originDeviceId: String = "",
    @ColumnInfo(name = "sync_state") @SerialName("sync_state") val syncState: SyncState = SyncState.PENDING
)

@Serializable
@Entity(
    tableName = "audit_logs",
    indices = [Index("shop_id"), Index("user_id"), Index("entity_type")]
)
data class AuditLogEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "user_id") @SerialName("user_id") val userId: String,
    @ColumnInfo(name = "action") @SerialName("action") val action: String,
    @ColumnInfo(name = "entity_type") @SerialName("entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") @SerialName("entity_id") val entityId: String,
    @ColumnInfo(name = "details_json") @SerialName("details_json") val detailsJson: String? = null,
    @ColumnInfo(name = "timestamp") @SerialName("timestamp") val timestamp: Long = 0L
)

@Serializable
@Entity(
    tableName = "conflicts",
    indices = [Index("shop_id"), Index("stock_item_id")]
)
data class ConflictEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "shop_id") @SerialName("shop_id") val shopId: String,
    @ColumnInfo(name = "stock_item_id") @SerialName("stock_item_id") val stockItemId: String,
    @ColumnInfo(name = "txn_id_a") @SerialName("txn_id_a") val txnIdA: String,
    @ColumnInfo(name = "txn_id_b") @SerialName("txn_id_b") val txnIdB: String,
    @ColumnInfo(name = "status") @SerialName("status") val status: ConflictStatus = ConflictStatus.OPEN,
    @ColumnInfo(name = "resolved_by") @SerialName("resolved_by") val resolvedBy: String? = null,
    @ColumnInfo(name = "resolved_at") @SerialName("resolved_at") val resolvedAt: Long? = null,
    @ColumnInfo(name = "resolution_note") @SerialName("resolution_note") val resolutionNote: String? = null
)

@Serializable
@Entity(
    tableName = "app_meta"
)
data class AppMetaEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: Int = 1,
    @ColumnInfo(name = "device_code") @SerialName("device_code") val deviceCode: String,
    @ColumnInfo(name = "device_id") @SerialName("device_id") val deviceId: String,
    @ColumnInfo(name = "active_shop_id") @SerialName("active_shop_id") val activeShopId: String? = null,
    @ColumnInfo(name = "schema_version") @SerialName("schema_version") val schemaVersion: Int = 1,
    @ColumnInfo(name = "receipt_seq") @SerialName("receipt_seq") val receiptSeq: Int = 1
)

@Serializable
@Entity(
    tableName = "sync_outbox"
)
data class SyncOutboxEntity(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "entity_type") @SerialName("entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") @SerialName("entity_id") val entityId: String,
    @ColumnInfo(name = "op") @SerialName("op") val op: SyncOp,
    @ColumnInfo(name = "payload_json") @SerialName("payload_json") val payloadJson: String,
    @ColumnInfo(name = "created_at") @SerialName("created_at") val createdAt: Long
)

@Serializable
@Entity(
    tableName = "sync_cursor"
)
data class SyncCursorEntity(
    @PrimaryKey @ColumnInfo(name = "collection_path") @SerialName("collection_path") val collectionPath: String,
    @ColumnInfo(name = "last_pulled_at") @SerialName("last_pulled_at") val lastPulledAt: Long
)
