android {
    signingConfigs {
        create("release") {
            keyAlias = "your-key-alias"
            keyPassword = "your-key-password"
            storeFile = file("path/to/release.keystore")
            storePassword = "your-store-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}