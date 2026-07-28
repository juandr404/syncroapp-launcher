// Configuracion de repositorios para los plugins de Gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Configuracion centralizada de repositorios de dependencias.
// FAIL_ON_PROJECT_REPOS obliga a que ningun modulo declare sus propios repos.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SyncroAppLauncher"

// Modulos del proyecto (ver docs/adr/ADR-004 para la justificacion de la estructura)
include(":app")
include(":core:ui")
include(":core:data")
include(":core:launcherapps")
