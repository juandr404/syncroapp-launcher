import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Datos de firma, leidos de keystore.properties si existe.
 *
 * Ese archivo y el almacen de claves estan en .gitignore y NUNCA se publican: quien tenga la
 * clave puede firmar actualizaciones que Android aceptara como legitimas. En CI los mismos
 * valores llegan por variables de entorno desde los secretos del repositorio.
 *
 * Si no hay ninguna de las dos fuentes, la variante de release se compila sin firmar. Eso es
 * deliberado: preferible un APK que no se instala a uno firmado con una clave de juguete que
 * despues no se puede reemplazar sin desinstalar la app del usuario.
 */
val propiedadesFirma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use(::load)
}

fun datoDeFirma(clave: String, variableEntorno: String): String? =
    propiedadesFirma.getProperty(clave) ?: System.getenv(variableEntorno)

android {
    namespace = "dev.syncroapp.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.syncroapp.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val ruta = datoDeFirma("storeFile", "SIGNING_STORE_FILE")
            val almacen = ruta?.let(::file)

            // Solo se configura si el almacen existe de verdad. Apuntar a un archivo ausente
            // hace fallar la compilacion con un error que no dice que falta la clave.
            if (almacen?.exists() == true) {
                storeFile = almacen
                storePassword = datoDeFirma("storePassword", "SIGNING_STORE_PASSWORD")
                keyAlias = datoDeFirma("keyAlias", "SIGNING_KEY_ALIAS")
                keyPassword = datoDeFirma("keyPassword", "SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Sin sufijo de applicationId: un launcher con dos IDs distintos
            // confunde al selector de "app de inicio" del sistema.
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release").takeIf {
                it.storeFile != null
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:launcherapps"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
