plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("maven-publish")
}
/*afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                // Replace with your library's details
                groupId = "com.helikanonlib"
                artifactId = "admanager"
                version = "5.072"

                val aarFile = layout.buildDirectory.file("outputs/aar/${project.name}-release.aar")
                artifact(aarFile.get().asFile)
            }
        }
    }
}*/

publishing{
    publications{
        register<MavenPublication>("release"){
            groupId = "com.helikanonlib"
            artifactId = "admanager"
            version = "5.088"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

/*publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
*/
android {
    namespace = "com.helikanonlib.admanager"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        manifestPlaceholders["admobAppId"] = "\${admobAppId}"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    //implementation "androidx.lifecycle:lifecycle-common-java8:2.3.0"
    // kapt("androidx.lifecycle:lifecycle-compiler:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.9.2")

    implementation("com.google.firebase:firebase-crashlytics-buildtools:3.0.6") // NativeTemplateStyle.java içinde bulunan @CanIgnoreReturnValue annotation için ekledik

    // google service
    implementation("com.google.android.gms:play-services-ads:24.5.0")
    implementation("com.google.android.gms:play-services-appset:16.1.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.2.0")

    // implementation(project(path = ":nativetemplates"))

    // APPLOVIN
    implementation("com.applovin:applovin-sdk:+")
    implementation("com.applovin.mediation:fyber-adapter:+")
    implementation("com.applovin.mediation:google-ad-manager-adapter:+")
    implementation("com.applovin.mediation:google-adapter:+")
    implementation("com.applovin.mediation:vungle-adapter:+")
    implementation("com.applovin.mediation:facebook-adapter:+")
    implementation("com.applovin.mediation:mintegral-adapter:+")
    implementation("com.applovin.mediation:bytedance-adapter:+")
    implementation("com.applovin.mediation:unityads-adapter:+")

}