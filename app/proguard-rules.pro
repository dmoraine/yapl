# Add project specific ProGuard rules here.

# Hilt
-keepclassmembers,allowobfuscation class * {
  @com.google.dagger.* <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep data classes used by Room
-keep class dev.pilotlog.data.database.entity.** { *; }
