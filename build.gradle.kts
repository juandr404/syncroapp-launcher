// Build raiz: declara los plugins disponibles para los modulos y configura el analisis estatico.
// "apply false" = se cargan aqui, pero cada modulo decide si los aplica.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt)
}

/**
 * Detekt se aplica a todos los modulos desde la raiz, con una sola configuracion.
 *
 * Se usa detekt-formatting (que embebe ktlint) en vez de agregar ktlint como herramienta
 * aparte: dos linters de estilo compitiendo terminan contradiciendose.
 */
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        // El proyecto es de una sola persona: que el build falle por un warning de estilo
        // interrumpe mas de lo que ayuda. Los reportes se revisan, no bloquean.
        ignoreFailures = true
        source.setFrom("src/main/java", "src/test/java")
    }

    dependencies {
        add("detektPlugins", rootProject.libs.detekt.formatting)
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "17"
        // Correccion automatica de estilo bajo demanda: ./gradlew detekt -PdetektAutoCorrect
        // No se activa siempre para que un analisis nunca modifique codigo por sorpresa.
        autoCorrect = providers.gradleProperty("detektAutoCorrect").isPresent
        reports {
            html.required.set(true)
            sarif.required.set(false)
            md.required.set(false)
        }
    }
}
