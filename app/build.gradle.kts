import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "app.privatemoney"
    compileSdk = 37

    val rawAnonKey = System.getenv("SUPABASE_ANON_KEY") ?: ""
    val b64AnonKey = if (rawAnonKey.isNotBlank()) {
        Base64.getEncoder().encodeToString(rawAnonKey.toByteArray(Charsets.UTF_8))
    } else ""
    val supabaseUrl = System.getenv("SUPABASE_URL") ?: "https://zahzagnindkkttdpmqii.supabase.co"

    defaultConfig {
        applicationId = "app.privatemoney.os"
        minSdk = 31
        targetSdk = 36
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 4
        versionName = System.getenv("VERSION_NAME") ?: "0.2.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_ANON_KEY_B64", "\"${b64AnonKey}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl}\"")
    }

    // Release signing comes only from environment variables set by the release workflow.
    // Without them the release build is intentionally left unsigned and CI rejects it.
    val keystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        disable += setOf("InvalidFragmentVersionForActivityResult")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation(libs.kotlin.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
