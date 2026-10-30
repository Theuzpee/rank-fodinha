# Regras do R8 para a versão de release do Rank Fodinha.

# kotlinx.serialization: mantém os serializadores gerados para os modelos @Serializable.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rankfodinha.app.**$$serializer { *; }
-keepclassmembers class com.rankfodinha.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.rankfodinha.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
