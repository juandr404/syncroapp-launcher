# Reglas de R8 para la build de release.

# kotlinx.serialization genera serializadores por reflexion sobre las clases @Serializable.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class dev.syncroapp.launcher.core.data.modelo.** {
    *** Companion;
}
-keepclasseswithmembers class dev.syncroapp.launcher.core.data.modelo.** {
    kotlinx.serialization.KSerializer serializer(...);
}
