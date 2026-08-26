plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val deepSeekApiKey = providers.environmentVariable("DEEPSEEK_API_KEY")
    .orElse("")
    .get()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.andrew.hamsterpet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.andrew.hamsterpet"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "2.6"
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepSeekApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
