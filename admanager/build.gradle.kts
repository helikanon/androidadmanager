import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            version = "5.100"

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
    publishing {
        singleVariant("release")
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
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // implementation("com.google.firebase:firebase-crashlytics-buildtools:3.0.6") // NativeTemplateStyle.java içinde bulunan @CanIgnoreReturnValue annotation için ekledik

    // google service
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.gms:play-services-appset:16.1.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.3.0")

    // implementation(project(path = ":nativetemplates"))

    // APPLOVIN
    implementation("com.applovin:applovin-sdk:13.6.3")
    implementation("com.applovin.mediation:fyber-adapter:8.4.7.0")
    implementation("com.applovin.mediation:google-ad-manager-adapter:25.4.0.0")
    implementation("com.applovin.mediation:google-adapter:25.4.0.0")
    implementation("com.applovin.mediation:vungle-adapter:7.7.7.1")
    implementation("com.applovin.mediation:facebook-adapter:6.21.0.0")
    implementation("com.applovin.mediation:mintegral-adapter:17.1.71.0")
    implementation("com.applovin.mediation:bytedance-adapter:8.2.0.4.0"){
        exclude(module = "tiktok-business-android-sdk-comp")
    }
    implementation("com.applovin.mediation:unityads-adapter:4.19.0.1")

    // inmobi begin
    implementation("com.applovin.mediation:inmobi-adapter:11.4.0.0")
    implementation("com.squareup.picasso:picasso:2.8")
    // implementation("androidx.recyclerview:recyclerview:1.1.0") // zaten yukarıda ekli
    // inmobi end

}
