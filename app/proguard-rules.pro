# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Gson & Firebase Data Models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class com.mmushtaq04.buysell.data.local.entity.** { *; }
-keepclassmembers class com.mmushtaq04.buysell.domain.model.** { *; }

# Keep Play Billing & Credential Manager
-keep class com.android.billingclient.api.** { *; }
-keep class androidx.credentials.** { *; }
