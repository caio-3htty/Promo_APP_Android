pluginManagement {
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

rootProject.name = "promo_APP_Android"
include(":app")
include(":core")
include(":data")
include(":feature-auth")
include(":feature-obras")
include(":feature-pedidos")
include(":feature-estoque")