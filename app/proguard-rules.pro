# Debug builds do not use R8. Rules for a future release build:
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class com.matchly.app.data.model.** { *** Companion; }
-keepclasseswithmembers class com.matchly.app.data.model.** { kotlinx.serialization.KSerializer serializer(...); }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
