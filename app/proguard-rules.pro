# Contenido temporal para prueba
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepattributes InnerClasses     # Útil para clases internas/anónimas
-keepattributes EnclosingMethod  # Útil para clases i

 # With R8 full mode generic signatures are stripped for classes that are not
 # kept. Suspend functions are wrapped in continuations where the type argument
 # is used.
 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Mantener Metadatos de Kotlin (Esencial para muchas funcionalidades de Kotlin)
-keep class kotlin.Metadata { *; }

# Mantener Clases y Miembros relacionados con Coroutines (Buena práctica)
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

# Añade aquí la regla -keep para la clase de datos específica si sabes cuál podría ser
# Ejemplo: -keep class com.cie10.data.model.Usuario { *; }
# O la regla amplia del paquete si es necesaria:
-keep class com.appgrouplab.firstlast.model.** { *; }


# --- Sección para kotlinx.serialization ---
# Descomenta o usa estas si utilizas KotlinxSerializationConverterFactory.
# El plugin de Gradle a menudo añade reglas, pero estas son recomendadas si tienes problemas.
# La regla `-keep class com.tuempresa.tuapp.modelo.** { *; }` de arriba es crucial.
-keep class **$$serializer { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

-keep class com.google.android.gms.ads.** { *; }
            -keep class com.google.android.gms.internal.ads.** { *; }
            -dontwarn com.google.android.gms.internal.ads.**
