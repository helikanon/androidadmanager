plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("applovin-quality-service")
    //id("maven-publish")
}

applovin {
    apiKey = "yOGLuhTmu0KZ0hihHeBgrZlHF622HDt9VSgLv1WrypvzRupBc5ZH141KQvOmP_4oXHtUG2DSFDaB5utqcD_XyT"
}

android {
    namespace = "com.helikanonlibsample.admanager"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.helikanonlibsample.admanager"
        minSdk = 24
        targetSdk = 34
        versionCode = 72
        versionName = "5.072"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"

    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }


}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    implementation(project(path = ":admanager"))

}