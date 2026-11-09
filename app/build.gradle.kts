plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hongducdev.toolkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hongducdev.toolkit"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3:1.4.0-alpha08")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Networking cho tìm kiếm địa điểm & lộ trình
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")

    // MapLibre Native Android SDK: bản đồ nền cho màn hình Android Auto trên điện thoại
    implementation("org.maplibre.gl:android-sdk:11.7.0")

    // Android Auto (Car App Library): màn hình điều hướng trên đầu xe
    implementation("androidx.car.app:app:1.4.0")

    // Quét mã QR & Barcode (CameraX + ML Kit độc lập offline & Google Play Services Code Scanner)
    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.guava:guava:33.3.1-android")

    // DIY Automation: JSON persistence & drag-and-drop reorderable action sequence
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("sh.calvin.reorderable:reorderable:2.4.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    compileOnly(files("libs/api-82.jar"))

    testImplementation("junit:junit:4.13.2")
}
