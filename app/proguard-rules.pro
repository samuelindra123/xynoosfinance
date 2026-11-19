# Add project specific ProGuard rules here.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# XynoosFinance Firestore, Room, & Domain models
-keep class com.example.data.repository.FirestoreTransactionDoc { *; }
-keep class com.example.domain.** { *; }
-keep class com.example.data.local.** { *; }

# Firebase Auth, Firestore, App Check & Firebase AI Logic
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.appcheck.** { *; }
-keep class com.google.firebase.ai.** { *; }

# Jetpack Credential Manager & Google Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Kotlin Serialization / Coroutines / Reflection safety
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**

