import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: ""

android {
    namespace = "com.antigravity.geofencing"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antigravity.geofencing"
        minSdk = 24 // Reasonable minimum for modern comparisons, though MVP assumes older style
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        manifestPlaceholders["appName"] = "Geofencing Evolution"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("mvp") {
            dimension = "version"
            applicationIdSuffix = ".mvp"
            versionNameSuffix = "-mvp"
            manifestPlaceholders["appName"] = "Geofence MVP"
        }
        create("mvvmLiveData") {
            dimension = "version"
            applicationIdSuffix = ".mvvm.livedata"
            versionNameSuffix = "-mvvm-livedata"
            manifestPlaceholders["appName"] = "Geofence MVVM LiveData"
        }
        create("mvi") {
            dimension = "version"
            applicationIdSuffix = ".mvi"
            versionNameSuffix = "-mvi"
            manifestPlaceholders["appName"] = "Geofence MVI"
        }
        create("mvvmCoroutines") {
            dimension = "version"
            applicationIdSuffix = ".mvvm.coroutines"
            versionNameSuffix = "-mvvm-coroutines"
            manifestPlaceholders["appName"] = "Geofence Coroutines"
        }
        create("mvvmFlow") {
            dimension = "version"
            applicationIdSuffix = ".mvvm.flow"
            versionNameSuffix = "-mvvm-flow"
            manifestPlaceholders["appName"] = "Geofence Flow"
        }
        create("machine") {
            dimension = "version"
            applicationIdSuffix = ".machine"
            versionNameSuffix = "-machine"
            manifestPlaceholders["appName"] = "Geofence Machine"
        }
        create("metaphysics") {
            dimension = "version"
            applicationIdSuffix = ".metaphysics"
            versionNameSuffix = "-metaphysics"
            manifestPlaceholders["appName"] = "Geofence Metaphysics"
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
        dataBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Compose (Global to satisfy compiler plugin, R8 will strip unused in release)
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.0")

    // RxJava (For MVI)
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    
    // WorkManager (For MVVM Coroutines)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
}

kapt {
    correctErrorTypes = true
}
