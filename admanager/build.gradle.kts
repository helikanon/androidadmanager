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
            version = "5.077"

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
    compileSdk = 34

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

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    //implementation "androidx.lifecycle:lifecycle-common-java8:2.3.0"
    // kapt("androidx.lifecycle:lifecycle-compiler:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.3")


    // google service
    implementation("com.google.android.gms:play-services-ads:23.2.0")
    implementation("com.google.android.gms:play-services-appset:16.1.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.1.0")

    // implementation(project(path = ":nativetemplates"))

    // APPLOVIN
    // APPLOVIN
    implementation("com.applovin:applovin-sdk:12.5.0")
    // implementation("com.applovin.mediation:adcolony-adapter:4.8.0.4") // applovin 12.2.0 sürümünde kaldırılmıştı sitesinden
    implementation("com.applovin.mediation:fyber-adapter:8.2.7.1")
    implementation("com.applovin.mediation:google-adapter:23.2.0.0")
    implementation("com.applovin.mediation:google-ad-manager-adapter:23.2.0.0")
    implementation("com.applovin.mediation:ironsource-adapter:8.1.0.0.0")
    implementation("com.applovin.mediation:facebook-adapter:6.17.0.0")
    implementation("com.applovin.mediation:mintegral-adapter:16.7.81.0") // admostla beraber sorun çıkıyo
    implementation("com.applovin.mediation:bytedance-adapter:5.9.1.1.0")
    implementation("com.applovin.mediation:unityads-adapter:4.12.1.0")
    implementation("com.applovin.mediation:vungle-adapter:7.4.0.0")

}