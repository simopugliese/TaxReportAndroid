plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    // Se usi Compose, assicurati che il plugin kotlin-compose sia attivo se la versione di AS lo richiede,
    // ma con le versioni recenti è spesso incluso nel plugin kotlin-android o configurato automaticamente.
}

android {
    namespace = "it.simonepugliese.taxreport"
    compileSdk = 36

    defaultConfig {
        applicationId = "it.simonepugliese.taxreport"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // NECESSARIO per librerie pesanti come MariaDB/SMBJ
        multiDexEnabled = true
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

    // [IMPORTANTE] Il backend richiede Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // [IMPORTANTE] Risolve conflitti di licenze nei JAR importati
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
    }

    // Configurazione specifica per abilitare Compose
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- 1. IL TUO BACKEND ---
    // Assicurati di aver copiato il file nella cartella app/libs/
    implementation(files("libs/TaxReport-1.0-v17-SNAPSHOT.jar"))

    // --- 2. DIPENDENZE DEL BACKEND (Java) ---
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    implementation("com.hierynomus:smbj:0.13.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("com.github.tony19:logback-android:3.0.0")

    // --- 3. UI JETPACK COMPOSE ---
    // Definiamo il BOM per gestire le versioni
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // Icone extra

    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- 4. DIPENDENZE BASE (Mantieni le tue esistenti) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat) // Opzionale con Compose, ma male non fa

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}