plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.healthmate"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.healthmate"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            // Signing is configured in Android Studio through
            // Build > Generate Signed App Bundle / APK (see README.md).
            // No keystore or password is stored in this project.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // UI
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)

    // Navigation (single activity + fragments + bottom navigation)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // MVVM state management
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Room (SQLite) with Java annotation processing
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Local unit tests (JVM)
    testImplementation(libs.junit)

    // Instrumentation tests (emulator / device)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.arch.core.testing)
}