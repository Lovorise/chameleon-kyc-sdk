plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mtchship.chameleon.kyc"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            kotlin.srcDirs("kyc", "internal")
        }
    }

    buildFeatures {
        compose = true
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
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")

    val cam = "1.4.1"
    implementation("androidx.camera:camera-core:$cam")
    implementation("androidx.camera:camera-camera2:$cam")
    implementation("androidx.camera:camera-lifecycle:$cam")
    implementation("androidx.camera:camera-view:$cam")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
}
