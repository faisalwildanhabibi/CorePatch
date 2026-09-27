pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    // PREFER_SETTINGS: repo di settings diutamakan, tapi plugin boleh tambah repo sendiri
    // Lebih kompatibel dengan plugin pihak ketiga seperti ZygoteLoader
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenLocal {
            content {
                includeGroup("io.github.libxposed")
            }
        }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Core Patch"
include(":app")
include(":zygisk")
