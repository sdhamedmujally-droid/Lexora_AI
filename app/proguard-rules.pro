# Lexis Release Packaging & ProGuard / R8 Rules

# Preserve Firestore data models, no-arg constructors, and annotations
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.data.remote.** { *; }
-keep class com.example.data.local.** { *; }

# Keep Room generated implementations
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Preserve Firebase & Credential Manager reflection entry points
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}
