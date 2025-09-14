plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.geogeusserclone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.geoguessrclone"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Performance Optimierungen
        multiDexEnabled = true

        // ProGuard optimierte Konfiguration
        ndk {
            debugSymbolLevel = "SYMBOL_TABLE"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Aktiviert für bessere Performance
            isShrinkResources = true // Entfernt unbenutzte Ressourcen
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://your-app.vercel.app\"")
            buildConfigField("String", "SOCKET_URL", "\"wss://your-app.vercel.app\"")

            // Performance Optimierungen für Release
            isDebuggable = false
            renderscriptOptimLevel = 3
        }
        debug {
            buildConfigField("String", "BASE_URL", "\"https://your-app.vercel.app\"")
            buildConfigField("String", "SOCKET_URL", "\"wss://your-app.vercel.app\"")

            // Debug Performance Optimierungen
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // Upgrade für bessere Performance
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"

        // Kotlin Compiler Optimierungen
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xjvm-default=all", // Bessere Java Interop Performance
            "-Xbackend-threads=4" // Parallele Compilation
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true

        // Deaktiviere unbenutzte Features für bessere Build Performance
        viewBinding = false
        dataBinding = false
        aidl = false
        renderScript = false
        shaders = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }

    // Build Performance Optimierungen
    androidResources {
        // generateLocaleConfig = true - Entfernt da resources.properties fehlt
    }

    // Kompilierungs-Cache Optimierungen
    compileSdk = 35

    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    // Core Library Desugaring - MUSS ZUERST STEHEN
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Core Android & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("androidx.compose.ui:ui:1.5.0") // or newer
    implementation("androidx.compose.ui:ui-tooling:1.5.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation("androidx.compose.material3:material3:1.2.0")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // Image Loading
    implementation(libs.coil.compose)

    // OSM Maps
    implementation(libs.osmdroid.android)

    // WorkManager für Background Tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")

    // Socket.IO Client for Android
    implementation("io.socket:socket.io-client:2.0.0") { exclude(group = "org.json", module = "json") }
    implementation("org.json:json:20210307")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
