plugins {
    id("com.android.library")
    id("maven-publish")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.uwonham.firebaselogin"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose =true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.1.0"  // Use the latest stable version
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")

    // Compose dependencies
    implementation (platform("androidx.compose:compose-bom:2025.11.01"))
    implementation ("androidx.compose.ui:ui")
    implementation ("androidx.compose.ui:ui-graphics")
    implementation ("androidx.compose.ui:ui-tooling-preview")
    implementation ("androidx.compose.material3:material3")

    // For Compose-Lifecycle integration
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.0")

    implementation("com.google.firebase:firebase-auth:24.0.1")

    implementation("com.google.firebase:firebase-firestore:26.0.2")
implementation("com.google.firebase:firebase-messaging:25.0.1")
    //hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    kapt("com.google.dagger:hilt-android-compiler:2.57.2")
    //Icons
    implementation ("androidx.compose.material:material-icons-extended:1.7.8")


    // For Compose testing
    debugImplementation ("androidx.compose.ui:ui-tooling")
    debugImplementation ("androidx.compose.ui:ui-test-manifest")
    testImplementation ("androidx.compose.ui:ui-test-junit4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["release"])
                groupId = "com.t1h2h0"
                artifactId = "firebaselogin"
                version = "1.1.63"

            }
        }
    }
}
