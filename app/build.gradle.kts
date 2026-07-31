plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.cbkii.noflyers"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.cbkii.noflyers"
        minSdk = 28
        targetSdk = 36
        versionCode = 15
        versionName = "0.1.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.all {
            it.useJUnit()
        }
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    testImplementation("junit:junit:4.13.2")
}
