# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.campus.platform.**$$serializer { *; }
-keepclassmembers class com.campus.platform.** { *** Companion; }
-keepclasseswithmembers class com.campus.platform.** { kotlinx.serialization.KSerializer serializer(...); }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# ── 项目自身数据类 ──────────────────────────────────────────
# 保护序列化/反序列化所需的所有 data class（Kotlinx Serialization / Room / Gson）
-keep class com.campus.platform.data.** { *; }
-keep class com.campus.platform.model.** { *; }
-keep class com.campus.platform.entity.** { *; }

# 保留 Kotlin data class 的 componentN / copy / toString（反射用）
-keepclassmembers class com.campus.platform.** {
    *** component*();
    *** copy(...);
    public java.lang.String toString();
}

# Supabase Kotlin SDK
-keep class io.github.jan-tennert.supabase.** { *; }
-dontwarn io.github.jan-tennert.supabase.**

# SQLCipher
-keep class net.zetetic.** { *; }
-keep class org.sqlite.** { *; }
-dontwarn net.zetetic.**
-dontwarn org.sqlite.**
