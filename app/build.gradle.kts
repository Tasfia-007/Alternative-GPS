plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.osmdroid.android.v6112)
    implementation(libs.okhttp)
    implementation (libs.play.services.maps.v1701) // For Google Maps integration if needed
    implementation (libs.okhttp)

    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.car.ui.lib)
    implementation(libs.play.services.location)
    androidTestImplementation(libs.ext.junit)

    androidTestImplementation(libs.espresso.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    implementation(libs.jsoup)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.gson)

    implementation(libs.glide)
    implementation(libs.opencsv)
    implementation(libs.osmdroid.android.v6111)
    implementation(libs.osmdroid.android.v6112)
    implementation(libs.okhttp.v493)
    implementation(libs.jsoup)





}