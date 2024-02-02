// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false


    id("com.google.gms.google-services") version "4.3.14" apply false
    id("com.android.library") version "8.2.0" apply false

    // id ("com.applovin.quality.AppLovinQualityServiceGradlePlugin") version "5.1.2" apply false

}

buildscript {
    val agp_version by extra("7.4.2")
    val agp_version1 by extra("8.2.2")
    val agp_version2 by extra("8.0.0")
    val agp_version3 by extra("8.2.0")
    repositories {
        maven { url = uri("https://artifacts.applovin.com/android") }
    }
    dependencies {
        classpath ("com.applovin.quality:AppLovinQualityServiceGradlePlugin:+")
    }
}