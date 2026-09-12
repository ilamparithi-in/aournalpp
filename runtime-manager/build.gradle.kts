plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.ilamparithi.aournalpp.runtime"
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
        val jv = JavaVersion.toVersion(libs.versions.javaVersion.get())
        sourceCompatibility = jv
        targetCompatibility = jv
    }
}

val usePrebuiltX11 = (providers.gradleProperty("usePrebuiltX11").orNull?.toBoolean()
    ?: rootProject.file("libs/x11-core-release.aar").exists())

dependencies {
    if (usePrebuiltX11) {
        implementation(files(rootProject.file("libs/x11-core-release.aar")))
    } else {
        implementation(project(":x11-core"))
    }
    implementation(libs.androidx.preference)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.commons.compress)
    implementation(libs.xz)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
