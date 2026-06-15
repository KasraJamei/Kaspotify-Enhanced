# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Room entities/DAOs and our data models (referenced by generated Room code)
-keep class com.example.kaspotify.data.local.** { *; }
-keep class com.example.kaspotify.data.model.** { *; }

# Hilt / Dagger generated code (most rules ship with the libraries; these are belt-and-suspenders)
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlin metadata & coroutines
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

# Keep enums (we switch/serialize a few by ordinal/name)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
