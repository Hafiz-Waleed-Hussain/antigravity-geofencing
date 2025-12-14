plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

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
        }
        create("mvvmLiveData") {
            dimension = "version"
            applicationIdSuffix = ".mvvm.livedata"
            versionNameSuffix = "-mvvm-livedata"
        }
        create("mvi") {
            dimension = "version"
            applicationIdSuffix = ".mvi"
            versionNameSuffix = "-mvi"
        }
        create("mvvmCoroutines") {
            dimension = "version"
            applicationIdSuffix = ".mvvm.coroutines"
            versionNameSuffix = "-mvvm-coroutines"
        }
        create("mvvmFlow") {
            dimension = "version"
            applicationIdSuffix = ".mvvm.flow"
            versionNameSuffix = "-mvvm-flow"
        }
        create("machine") {
            dimension = "version"
            applicationIdSuffix = ".machine"
            versionNameSuffix = "-machine"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Compose (Used in mvvmFlow and potentially machine)
    val composeBom = platform("androidx.compose:compose-bom:2023.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

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
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
