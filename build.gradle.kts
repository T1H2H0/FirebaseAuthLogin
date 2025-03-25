plugins {
    id("com.android.library")
    id("maven-publish")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt") // Add kapt for annotation processing
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.uwonham.firebaselogin"
    compileSdk = 35

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose =true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.1.0"  // Use the latest stable version
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Compose dependencies
    implementation (platform("androidx.compose:compose-bom:2025.03.00"))
    implementation ("androidx.compose.ui:ui")
    implementation ("androidx.compose.ui:ui-graphics")
    implementation ("androidx.compose.ui:ui-tooling-preview")
    implementation ("androidx.compose.material3:material3")

    // For Compose-Lifecycle integration
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")

    implementation("com.google.firebase:firebase-auth:23.2.0")

    implementation("com.google.firebase:firebase-firestore:25.1.2")

//hilt
    implementation("com.google.dagger:hilt-android:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    kapt("com.google.dagger:hilt-android-compiler:2.52")
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
            release(MavenPublication) {
                from components.release

                        groupId = 'com.github.T1H2H0'
                artifactId = 'firebaselogin'
                version = '1.0'
            }
        }
    }
}
//publishing {
//    publications {
//        create<MavenPublication>("mavenJava") {
//            from(components["java"])
//
//            groupId = "com.uwonham"
//            artifactId = "firebaselogin"
//            version = "0.1.0"
//
////            // Optionally, if you have sources and Javadoc to publish
////            artifact(tasks["javadocJar"])
////            artifact(tasks["sourcesJar"])
//        }
//    }
//}
//
//    repositories {
//        maven {
//            name = "MavenRepo"
//            url = uri("https://your.repo.url/repository/maven-releases/")
//
//            credentials {
//                username = System.getenv("MAVEN_USERNAME")
//                password = System.getenv("MAVEN_PASSWORD")
//            }
//        }
//    }
//}
//    publishing {
//        publications {
//            create<MavenPublication>("release") {
//                from(components["release"])
//
//            groupId = "com.github.t1h2h0"
//                artifactId = "firebaselogin"
//                version = "1.0"
//            }
//
//        }
//    }
