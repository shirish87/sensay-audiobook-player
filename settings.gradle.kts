pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Sensay Audiobook Player"
include(":app")
include(":core:remote")
include(":core:config")
include(":core:media")
include(":core:data")
include(":core:scanner")
include(":core:lookup")
include(":core:icons")
