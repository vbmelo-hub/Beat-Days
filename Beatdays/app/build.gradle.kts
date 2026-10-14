import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val releaseSecrets = Properties().apply {
    val source = rootProject.file("signing/release.properties")
    if (source.exists()) source.inputStream().use { load(it) }
}

android {
    namespace = "com.example.beatdays"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.beatdays"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "1.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("production") {
            if (releaseSecrets.isNotEmpty()) {
                storeFile = rootProject.file(releaseSecrets.getProperty("storeFile"))
                storePassword = releaseSecrets.getProperty("storePassword")
                keyAlias = releaseSecrets.getProperty("keyAlias")
                keyPassword = releaseSecrets.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("production")
            isShrinkResources = true
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    lint {
        sarifReport = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    constraints {
        implementation(libs.core.ktx)
    }
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.ext.junit)
}