import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("applovin-quality-service")
}

applovin {
    apiKey = "yOGLuhTmu0KZ0hihHeBgrZlHF622HDt9VSgLv1WrypvzRupBc5ZH141KQvOmP_4oXHtUG2DSFDaB5utqcD_XyT"
}

android {
    namespace = "com.helikanonlibsample.admanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.helikanonlibsample.admanager"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 100
        versionName = "5.100"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"

    }
    /*publishing {
        singleVariant("release") {}
    }*/
    buildTypes {
        release {
            isShrinkResources = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }


}

dependencies {

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")


    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation(project(":admanager"))

}
