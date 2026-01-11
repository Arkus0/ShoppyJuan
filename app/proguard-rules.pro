# Add project specific ProGuard rules here.

# Keep Supabase classes
-keep class io.github.jan.supabase.** { *; }
-keepclassmembers class io.github.jan.supabase.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.arkus.shoppyjuan.**$$serializer { *; }
-keepclassmembers class com.arkus.shoppyjuan.** {
    *** Companion;
}
-keepclasseswithmembers class com.arkus.shoppyjuan.** {
    kotlinx.serialization.KSerializer serializer(...);
}
