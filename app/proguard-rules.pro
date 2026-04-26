# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.empiretycoon.game.**$$serializer { *; }
-keepclassmembers class com.empiretycoon.game.** {
    *** Companion;
}
-keepclasseswithmembers class com.empiretycoon.game.** {
    kotlinx.serialization.KSerializer serializer(...);
}
