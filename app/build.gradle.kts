plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.kapt")
    // KSP plugin removed (was causing issues). If you need KSP later, re-add the plugin here and a
    // matching version in `settings.gradle.kts` or a pluginManagement block.
}

android {
    namespace = "com.example.musicplayer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.musicplayer"
        minSdk = 36
        targetSdk = 37
        // Use centralized version properties (defined in gradle.properties)
        versionCode = (project.findProperty("VERSION_CODE") as String).toInt()
        versionName = (project.findProperty("VERSION_NAME") as String)
        // Expose version info via BuildConfig for compile-time access
        buildConfigField("String", "APP_VERSION_NAME", "\"${project.findProperty("VERSION_NAME") as String}\"")
        buildConfigField("int", "APP_VERSION_CODE", (project.findProperty("VERSION_CODE") as String))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        // Enable generation of BuildConfig fields (we use buildConfigField to expose app version)
        buildConfig = true
    }
}

val material3_version = "1.4.0" // replace with the Material3 version used in your project

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Use explicit Material3 coordinate with the chosen version and add pullrefresh artifact
    implementation("androidx.compose.material3:material3:${material3_version}")

    implementation(libs.androidx.compose.material3)
    implementation("androidx.navigation:navigation-compose:2.6.0")
    implementation(libs.volley)
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.compose.material.icons.extended)

    // Background Color Palette
    implementation(libs.androidx.palette.ktx)

    implementation("androidx.core:core-splashscreen:1.0.1")

    // Networking / Serialization / Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // AndroidX Media3 (transition target for ExoPlayer) - conservative version
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    // Media session helper
    implementation("androidx.media:media:1.6.0")


    // For Slider
    implementation(libs.androidx.compose.material)

    // Image loading (Coil Compose) for remote favicons
    implementation("io.coil-kt:coil-compose:2.4.0")
// Or the latest stable version

    // DataStore for persisting preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Lifecycle compose helpers (LocalLifecycleOwner, etc.)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")

    // Room (DB) - runtime + ktx; Room's compiler was previously configured with KSP
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    // Room compiler: use KAPT to generate the Room implementation (AppDatabase_Impl)
    kapt("androidx.room:room-compiler:2.8.4")
    testImplementation("androidx.room:room-testing:2.8.4")
}
