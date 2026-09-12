pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

val isX11TaskRequested = gradle.startParameter.taskNames.any { it.contains("x11-core") }
val usePrebuiltX11 = if (isX11TaskRequested) {
    false
} else {
    providers.gradleProperty("usePrebuiltX11").orNull?.toBoolean()
        ?: file("libs/x11-core-release.aar").exists()
}

rootProject.name = "Aournal++"
include(":app")
if (!usePrebuiltX11) {
    include(":x11-core")
}
include(":runtime-manager")
include(":scripts")