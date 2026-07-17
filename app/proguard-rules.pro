# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# DTO classes (DeepSeek API)
-keep class com.example.expensetracker.data.remote.dto.** { *; }

# Entity classes
-keep class com.example.expensetracker.data.local.entity.** { *; }
