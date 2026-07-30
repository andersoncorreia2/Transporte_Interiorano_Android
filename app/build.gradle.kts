import java.util.Properties

plugins {
    id("com.google.gms.google-services")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.transporte_interiorano"
    compileSdk = 36

    // 🟢 Lê o token do secrets.properties para usar nos flavors
    val secretsFile = rootProject.file("secrets.properties")
    var mapboxTokenVal = ""
    if (secretsFile.exists()) {
        val properties = Properties()
        properties.load(secretsFile.reader())
        mapboxTokenVal = properties.getProperty("MAPBOX_API_TOKEN") ?: properties.getProperty("MAPBOX_TOKEN") ?: ""
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Injeta o token gerado pelo Gradle no BuildConfig de forma segura
        buildConfigField("String", "MAPBOX_TOKEN", "\"$mapboxTokenVal\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true // 👈 Obrigatório para gerar a classe BuildConfig
    }

    flavorDimensions += "modo"

    productFlavors {
        create("dev") {
            dimension = "modo"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "App Dev")
            buildConfigField("String", "BASE_URL", "\"https://obnoxious-audience-finite.ngrok-free.dev\"")
            buildConfigField("String", "MAPBOX_TOKEN", "\"$mapboxTokenVal\"")
        }
        create("prod") {
            dimension = "modo"
            resValue("string", "app_name", "Transporte Interiorano")
            buildConfigField("String", "BASE_URL", "\"https://transporte-interiorano-backend.onrender.com\"")
            buildConfigField("String", "MAPBOX_TOKEN", "\"$mapboxTokenVal\"")
        }
    }

    // ... resto do seu build.gradle.kts (buildTypes, buildFeatures, etc)

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
}