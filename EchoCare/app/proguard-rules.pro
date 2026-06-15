# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the default proguard-android.txt.

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class * { *; }

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Hilt
-keep class dagger.** { *; }
-keepclassmembers class * {
    @dagger.* <fields>;
    @javax.inject.* <fields>;
    @javax.inject.* <init>(...);
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Reminder entities
-keep class com.echocare.app.data.local.entity.** { *; }
-keep class com.echocare.app.domain.model.** { *; }
