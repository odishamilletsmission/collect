plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinKapt)
}

android {
    namespace = "org.odk.collect.wassan"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":icons"))
    implementation(project(":strings"))
    implementation(project(":location"))
    implementation(project(":androidshared"))
    implementation(project(":external-app"))
    implementation(project(":async"))
    implementation(project(":analytics"))
    implementation(project(":permissions"))
    implementation(project(":settings"))
    implementation(project(":maps"))
    implementation(project(":material"))
    implementation(project(":web-page"))
    implementation(project(":projects"))
    implementation(libs.kotlinStdlib)
    implementation(libs.androidxAppcompat)
    implementation(libs.androidxLifecycleLivedataKtx)
    implementation(libs.timber)
    implementation(libs.playServicesLocation)
    implementation(libs.androidxFragmentKtx)
    implementation(libs.dagger)
    kapt(libs.daggerCompiler)

    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxAppcompat)
    implementation(libs.androidMaterial)
    testImplementation(libs.junit)
    androidTestImplementation(libs.extJunit)
    androidTestImplementation(libs.androidxTestEspressoCore)
}