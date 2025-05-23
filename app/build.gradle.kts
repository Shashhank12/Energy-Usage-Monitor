import java.util.Properties
import java.io.FileInputStream

fun getApiKey(name: String): String {
    val properties = Properties()
    val inputStream = FileInputStream("local.properties")
    properties.load(inputStream)
    return properties.getProperty(name)
}

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "edu.sjsu.android.energyusagemonitor"
    compileSdk = 34

    defaultConfig {
        applicationId = "edu.sjsu.android.energyusagemonitor"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "UTILITY_API_KEY",
            "\"${getApiKey("UTILITY_API_KEY")}\""
        )

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${getApiKey("GEMINI_API_KEY")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.firebase:firebase-firestore-ktx")

    //for utilityapi
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")


    //for charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(libs.appcompat)
    implementation(libs.annotation)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.activity)
    implementation(libs.generativeai)

    //for photo picker
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    // for markdown
    implementation(libs.core)
    implementation(libs.html)
    implementation(libs.ext.strikethrough)
    implementation(libs.ext.tables)
    implementation(libs.linkify)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    testImplementation ("junit:junit:4.13.2")
    testImplementation ("org.mockito:mockito-core:5.8.0")
    testImplementation ("androidx.test:core:1.5.0")

    // for linear regression
    implementation ("org.apache.commons:commons-math3:3.6.1")

    apply(plugin = "com.google.gms.google-services")
}

