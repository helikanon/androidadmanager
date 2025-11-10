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
        versionCode = 91
        versionName = "5.091"

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
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }


}

dependencies {

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")


    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.gms:play-services-ads:24.7.0")
    implementation(project(":admanager"))

}