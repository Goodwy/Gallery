plugins {
    id("com.android.test")
    alias(libs.plugins.baselineProfile)
}

android {
    namespace = "com.goodwy.gallery.baselineprofile"
    compileSdk = libs.versions.app.build.compileSDKVersion.get().toInt()

    defaultConfig {
        minSdk = 28
        targetSdk = libs.versions.app.build.targetSDK.get().toInt()
    }

    targetProjectPath = ":app"

    flavorDimensions += "distribution"
    productFlavors {
        create("foss") {
            dimension = "distribution"
        }
    }

    experimentalProperties["android.experimental.self-instrumenting"] = true

    testOptions.managedDevices.localDevices {
        create("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "aosp-atd"
        }
    }
}

// This is the plugin configuration. Everything is optional. Defaults are in the
// baseline profile gradle plugin.
baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
