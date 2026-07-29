import java.util.Properties

plugins {
    id("com.google.gms.google-services")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.transporte_interiorano"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 🟢 LEITURA SEGURA DO MAPBOX
        val secretsFile = rootProject.file("secrets.properties")
        if (secretsFile.exists()) {
            val properties = Properties()
            properties.load(secretsFile.reader())

            val tokenReal = properties.getProperty("MAPBOX_API_TOKEN")
                ?: properties.getProperty("MAPBOX_TOKEN")

            if (tokenReal != null && tokenReal.isNotEmpty()) {
                buildConfigField("String", "MAPBOX_TOKEN", "\"$tokenReal\"")
            } else {
                throw GradleException("ERRO: A chave 'MAPBOX_API_TOKEN' ou 'MAPBOX_TOKEN' não foi encontrada no secrets.properties.")
            }
        } else {
            throw GradleException("ERRO: Arquivo secrets.properties não encontrado na raiz.")
        }
    }

    // 🟢 ATIVAÇÃO DOS PRODUCT FLAVORS
    flavorDimensions += "modo"

    productFlavors {
        create("dev") {
            dimension = "modo"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "App Dev")
        }
        create("prod") {
            dimension = "modo"
            resValue("string", "app_name", "Transporte Interiorano")
        }
    }

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