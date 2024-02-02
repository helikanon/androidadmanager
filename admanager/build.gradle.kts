plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("maven-publish")
}



android {
    namespace = "com.helikanonlib.admanager"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
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
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    //implementation "androidx.lifecycle:lifecycle-common-java8:2.3.0"
    // kapt("androidx.lifecycle:lifecycle-compiler:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")


    // google service
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    implementation("com.google.android.gms:play-services-appset:16.0.1")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.0")

    implementation(project(path = ":nativetemplates"))

    // APPLOVIN
    implementation("com.applovin:applovin-sdk:12.1.0")
    implementation("com.applovin.mediation:adcolony-adapter:4.8.0.4")
    implementation("com.applovin.mediation:fyber-adapter:8.2.5.0")
    implementation("com.applovin.mediation:google-adapter:22.6.0.0")
    implementation("com.applovin.mediation:google-ad-manager-adapter:22.6.0.0")
    implementation("com.applovin.mediation:ironsource-adapter:7.6.0.0.0")
    implementation("com.applovin.mediation:facebook-adapter:6.16.0.2")
    implementation("com.applovin.mediation:mintegral-adapter:16.6.31.0") // admostla beraber sorun çıkıyo
    implementation("com.applovin.mediation:bytedance-adapter:5.7.0.3.0")
    implementation("com.applovin.mediation:unityads-adapter:4.9.2.1")
    implementation("com.applovin.mediation:vungle-adapter:7.1.0.0")
}