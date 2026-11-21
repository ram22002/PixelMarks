
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pixelmarks.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pixelmarks.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Echo-style build identification: the commit this build was made from.
        // Lets the updater detect new beta builds of the same version by commit id.
        val commitSha = try {
            providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText.get().trim().ifBlank { "unknown" }
        } catch (_: Exception) {
            "unknown"
        }
        buildConfigField("String", "COMMIT_SHA", "\"$commitSha\"")

        // Distribution channel this build was published for (stable / beta / nightly).
        // Set as BUILD_CHANNEL env var by CI; local builds default to stable.
        val buildChannel = System.getenv("BUILD_CHANNEL") ?: "stable"
        buildConfigField("String", "BUILD_CHANNEL", "\"$buildChannel\"")

        buildConfigField(
            "String",
            "GOOGLE_OAUTH_CLIENT_ID",
            "\"793133201141-6rv522skm635b8i3jsm48ngros9d64oe.apps.googleusercontent.com\""
        )
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootDir}/keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            // 1. CI signing: secrets injected as environment variables.
            //    KEYSTORE_BASE = Base64-encoded keystore, KEY_ALIAS = key alias,
            //    PASSWORD = keystore & key password.
            //    (MIME decoder tolerates line-wrapped Base64.)
            val ksBase = System.getenv("KEYSTORE_BASE")
            val ksAlias = System.getenv("KEY_ALIAS")
            val ksPass = System.getenv("PASSWORD")

            // 2. Local signing: credentials in gitignored local.properties
            //    (release.keystoreFile / release.keystorePassword / release.keyAlias)
            val localProps = Properties().apply {
                val f = rootProject.file("local.properties")
                if (f.exists()) f.inputStream().use { load(it) }
            }
            val localStore = localProps.getProperty("release.keystoreFile")?.let { rootProject.file(it) }
            val localPass = localProps.getProperty("release.keystorePassword")
            val localAlias = localProps.getProperty("release.keyAlias")

            when {
                !ksBase.isNullOrBlank() && !ksAlias.isNullOrBlank() && !ksPass.isNullOrBlank() -> {
                    val ksFile = File(rootDir, "build/ci-release.keystore")
                    ksFile.parentFile.mkdirs()
                    ksFile.writeBytes(Base64.getMimeDecoder().decode(ksBase))
                    storeFile = ksFile
                    storePassword = ksPass
                    keyAlias = ksAlias
                    keyPassword = ksPass
                }
                localStore != null && localStore.exists() && !localPass.isNullOrBlank() && !localAlias.isNullOrBlank() -> {
                    storeFile = localStore
                    storePassword = localPass
                    keyAlias = localAlias
                    keyPassword = localPass
                }
                else -> {
                    // Last resort for fresh clones: debug keystore so release
                    // builds still package (not update-compatible with CI builds).
                    storeFile = file("${rootDir}/keystore/debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            versionNameSuffix = "-beta"
            isDebuggable = true
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.biometric)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.work.runtime.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking & HTML Parsing
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.gson)
    implementation(libs.zxing.core)

    // Image Loading (Coil 3)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
