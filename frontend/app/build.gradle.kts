import java.util.Properties

val localProps = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.taxipot"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.taxipot"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String", "KAKAO_NATIVE_APP_KEY",
            "\"${localProps.getProperty("KAKAO_NATIVE_APP_KEY")}\""
        )
        buildConfigField(
            "String", "KAKAO_REST_API_KEY",
            "\"${localProps.getProperty("KAKAO_REST_API_KEY", "")}\""
        )
        buildConfigField(
            "String", "BASE_URL",
            "\"${localProps.getProperty("BASE_URL", "http://10.0.2.2:8080/")}\""
        )
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("com.kakao.maps.open:android:2.12.18")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}