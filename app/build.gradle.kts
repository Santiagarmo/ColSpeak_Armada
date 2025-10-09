plugins {
    alias(libs.plugins.android.application)

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.speak"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.speak"
        minSdk = 28
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Agregar esta configuración para resolver el conflicto de META-INF
    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "META-INF/MANIFEST.MF"
            excludes += "META-INF/maven/**"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/INDEX.LIST.*"
        }
    }

    // Agregar esta configuración para incluir el modelo de Vosk
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Forzar versiones específicas para evitar conflictos
        force("org.hamcrest:hamcrest:2.2")
        force("org.hamcrest:hamcrest-core:2.2")
        force("org.hamcrest:hamcrest-library:2.2")
        force("junit:junit:4.13.2")
        
        // Excluir versiones antiguas de Hamcrest
        exclude(group = "org.hamcrest", module = "hamcrest-library")
        exclude(group = "org.hamcrest", module = "hamcrest-integration")
        exclude(group = "org.hamcrest", module = "hamcrest-core")
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    // Testing
    testImplementation("junit:junit:4.13.2") {
        exclude(group = "org.hamcrest")
    }
    testImplementation("org.hamcrest:hamcrest:2.2")
    androidTestImplementation(libs.ext.junit) {
        exclude(group = "org.hamcrest")
    }
    androidTestImplementation(libs.espresso.core) {
        exclude(group = "org.hamcrest")
    }

    // Stanford CoreNLP para procesamiento de lenguaje natural
    implementation("edu.stanford.nlp:stanford-corenlp:4.5.4") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.hamcrest")
    }
    implementation("edu.stanford.nlp:stanford-corenlp:4.5.4:models") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.hamcrest")
    }

    // Apache OpenNLP como alternativa ligera a NLTK
    implementation("org.apache.opennlp:opennlp-tools:2.3.0") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.hamcrest")
    }

    // TensorFlow Lite para modelos de ML
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")

    // Google Play Services (necesario para modo online)
    implementation("com.google.android.gms:play-services-base:18.3.0")
    
    // Modelos de lenguaje para reconocimiento
    implementation("com.google.mlkit:language-id:17.0.4")
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Vosk para reconocimiento de voz offline
    implementation("com.alphacephei:vosk-android:0.3.47") {
        exclude(group = "net.java.dev.jna")
    }
    implementation("net.java.dev.jna:jna:5.13.0@aar")

    // AndroidSVG para manejar archivos SVG
    implementation("com.caverock:androidsvg:1.4")

}