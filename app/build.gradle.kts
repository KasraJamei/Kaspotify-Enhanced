plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.kaspotify"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kaspotify"
        minSdk = 24
        targetSdk = 34
        versionCode = 24
        versionName = "1.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // A stable signing key is committed (kaspotify-signing.p12) so every build — including CI — is
    // signed identically. That's what lets a freshly built APK install *over* a previously installed
    // Kaspotify without "uninstall the old version first" (Android rejects signature mismatches).
    // It is deliberately NOT a secret-grade key (its password is public, it's for sideload updates).
    // For real Play Store signing, supply the KEYSTORE_* env/secrets and they take precedence.
    val secretKeystore = file("release.keystore")
    val useSecretKeystore = secretKeystore.exists() && System.getenv("KEYSTORE_PASSWORD") != null

    signingConfigs {
        create("stable") {
            storeFile = file("kaspotify-signing.p12")
            storePassword = "kaspotify"
            keyAlias = "kaspotify"
            keyPassword = "kaspotify"
            storeType = "PKCS12"
        }
        if (useSecretKeystore) {
            create("release") {
                storeFile = secretKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Share the stable key so local installs and CI builds carry one consistent signature.
            signingConfig = signingConfigs.getByName("stable")
        }
        release {
            // Minification stays OFF on purpose: a directly-installable build that is guaranteed to
            // run (no R8/reflection surprises with Hilt/Room/Media3) matters more than APK size here.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (useSecretKeystore) signingConfigs.getByName("release")
            else signingConfigs.getByName("stable")
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
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
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.palette)
}
