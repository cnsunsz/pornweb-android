plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pornweb.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pornweb.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("SIGNING_STORE_FILE")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
            }
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (System.getenv("SIGNING_STORE_FILE").isNullOrBlank()) {
                signingConfigs.getByName("debug")
            } else {
                signingConfigs.getByName("release")
            }
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
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil:2.7.0")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-datasource:1.5.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.5.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
