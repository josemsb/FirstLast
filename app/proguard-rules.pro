# ─── Kotlin ───────────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes InnerClasses,EnclosingMethod
-keepattributes *Annotation*

-keep class kotlin.Metadata { *; }
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keepclassmembers class * extends kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    <init>(kotlin.coroutines.Continuation);
    <fields>;
}
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.SuspendLambda {
    <init>(int, kotlin.coroutines.Continuation);
    <fields>;
}
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.RestrictedSuspendLambda {
    <init>(int, kotlin.coroutines.Continuation);
    <fields>;
}

# ─── kotlinx.serialization ────────────────────────────────────────────────────
-keep class **$$serializer { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    static ** Companion;
    static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ─── App model & data classes (Firestore field mapping) ───────────────────────
-keep class com.appgrouplab.firstlast.model.** { *; }
-keep class com.appgrouplab.firstlast.data.** { *; }
-keepclassmembers class com.appgrouplab.firstlast.** {
    public <init>();
    public <fields>;
}

# ─── Firebase ─────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.internal.firebase** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.internal.firebase**

# Firestore uses reflection to map document fields to POJO fields
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keepclassmembers class com.appgrouplab.firstlast.model.** {
    public <init>();
    public ** get*();
    public void set*(**);
    <fields>;
}

# ─── Google Play Services / AdMob ─────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.internal.ads.** { *; }
-dontwarn com.google.android.gms.internal.ads.**
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# Keep native ad view IDs used in native_ad.xml inflated by AdMob
-keepclassmembers class com.google.android.gms.ads.nativead.NativeAdView { *; }
-keepclassmembers class com.google.android.gms.ads.nativead.MediaView { *; }

# ─── WorkManager ──────────────────────────────────────────────────────────────
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─── Jetpack Compose ──────────────────────────────────────────────────────────
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# ─── AndroidX / Lifecycle ─────────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ─── Suppress common warnings ─────────────────────────────────────────────────
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
