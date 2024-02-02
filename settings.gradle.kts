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
        // mavenLocal()
        maven(url = "https://jitpack.io")

        // maven(url = "https://artifacts.applovin.com/android")
        maven(url = "https://android-sdk.is.com/")
        maven(url = "https://artifact.bytedance.com/repository/pangle")
        maven(url = "https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
    }
}

rootProject.name = "Helikanon AdManager"
include(":app")
include(":admanager")
include(":nativetemplates")
