package com.mmushtaq04.buysell.data.local

import androidx.room.TypeConverter
import com.mmushtaq04.buysell.data.local.enums.*

class Converters {
    @TypeConverter
    fun fromSyncState(value: SyncState?): String? = value?.name

    @TypeConverter
    fun toSyncState(value: String?): SyncState? = value?.let { enumValueOf<SyncState>(it) }

    @TypeConverter
    fun fromIdentifierType(value: IdentifierType?): String? = value?.name

    @TypeConverter
    fun toIdentifierType(value: String?): IdentifierType? = value?.let { enumValueOf<IdentifierType>(it) }

    @TypeConverter
    fun fromTrackingMode(value: TrackingMode?): String? = value?.name

    @TypeConverter
    fun toTrackingMode(value: String?): TrackingMode? = value?.let { enumValueOf<TrackingMode>(it) }

    @TypeConverter
    fun fromRole(value: Role?): String? = value?.name

    @TypeConverter
    fun toRole(value: String?): Role? = value?.let { enumValueOf<Role>(it) }

    @TypeConverter
    fun fromMemberStatus(value: MemberStatus?): String? = value?.name

    @TypeConverter
    fun toMemberStatus(value: String?): MemberStatus? = value?.let { enumValueOf<MemberStatus>(it) }

    @TypeConverter
    fun fromPartyTypeHint(value: PartyTypeHint?): String? = value?.name

    @TypeConverter
    fun toPartyTypeHint(value: String?): PartyTypeHint? = value?.let { enumValueOf<PartyTypeHint>(it) }

    @TypeConverter
    fun fromItemStatus(value: ItemStatus?): String? = value?.name

    @TypeConverter
    fun toItemStatus(value: String?): ItemStatus? = value?.let { enumValueOf<ItemStatus>(it) }

    @TypeConverter
    fun fromTxnType(value: TxnType?): String? = value?.name

    @TypeConverter
    fun toTxnType(value: String?): TxnType? = value?.let { enumValueOf<TxnType>(it) }

    @TypeConverter
    fun fromScope(value: Scope?): String? = value?.name

    @TypeConverter
    fun toScope(value: String?): Scope? = value?.let { enumValueOf<Scope>(it) }

    @TypeConverter
    fun fromPaymentDirection(value: PaymentDirection?): String? = value?.name

    @TypeConverter
    fun toPaymentDirection(value: String?): PaymentDirection? = value?.let { enumValueOf<PaymentDirection>(it) }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let { enumValueOf<PaymentMethod>(it) }

    @TypeConverter
    fun fromPaymentAccountType(value: PaymentAccountType?): String? = value?.name

    @TypeConverter
    fun toPaymentAccountType(value: String?): PaymentAccountType? = value?.let { enumValueOf<PaymentAccountType>(it) }

    @TypeConverter
    fun fromPromiseDirection(value: PromiseDirection?): String? = value?.name

    @TypeConverter
    fun toPromiseDirection(value: String?): PromiseDirection? = value?.let { enumValueOf<PromiseDirection>(it) }

    @TypeConverter
    fun fromPromiseStatus(value: PromiseStatus?): String? = value?.name

    @TypeConverter
    fun toPromiseStatus(value: String?): PromiseStatus? = value?.let { enumValueOf<PromiseStatus>(it) }

    @TypeConverter
    fun fromUploadState(value: UploadState?): String? = value?.name

    @TypeConverter
    fun toUploadState(value: String?): UploadState? = value?.let { enumValueOf<UploadState>(it) }

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory?): String? = value?.name

    @TypeConverter
    fun toExpenseCategory(value: String?): ExpenseCategory? = value?.let { enumValueOf<ExpenseCategory>(it) }

    @TypeConverter
    fun fromAuditAction(value: AuditAction?): String? = value?.name

    @TypeConverter
    fun toAuditAction(value: String?): AuditAction? = value?.let { enumValueOf<AuditAction>(it) }

    @TypeConverter
    fun fromConflictStatus(value: ConflictStatus?): String? = value?.name

    @TypeConverter
    fun toConflictStatus(value: String?): ConflictStatus? = value?.let { enumValueOf<ConflictStatus>(it) }

    @TypeConverter
    fun fromSyncOp(value: SyncOp?): String? = value?.name

    @TypeConverter
    fun toSyncOp(value: String?): SyncOp? = value?.let { enumValueOf<SyncOp>(it) }
}
