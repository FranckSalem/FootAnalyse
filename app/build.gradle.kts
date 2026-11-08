plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.salem.footanalyse"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.salem.footanalyse"
        minSdk = 33        // Android 13
        targetSdk = 36     // Android 16 (fonctionne aussi sur 14 et 15)
        // Numéro de version = numéro de la compilation GitHub (chaque mise à jour s'installe par-dessus l'ancienne).
        versionCode = project.findProperty("vc")?.toString()?.toIntOrNull() ?: 1
        versionName = "1." + (project.findProperty("vc")?.toString() ?: "0")
        // Téléphones Android récents (dont Samsung S21) : réduit la taille de l'APK
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    // Clé de signature fixe : les mises à jour s'installent sans désinstaller (le journal est conservé).
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
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
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    // Reconnaissance de texte sur le téléphone (lecture des captures d'écran)
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
