import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropsFile = rootDir.resolve("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

val resolvedKeystoreFilePath = System.getenv("KEYSTORE_FILE")
    ?: (project.findProperty("KEYSTORE_FILE") as? String)
    ?: localProperties.getProperty("release.keystore.path")
val resolvedKeystoreFile = resolvedKeystoreFilePath?.let { file(it) }

val resolvedStorePassword = System.getenv("KEYSTORE_PASSWORD")
    ?: (project.findProperty("KEYSTORE_PASSWORD") as? String)
    ?: localProperties.getProperty("release.keystore.password")
val resolvedKeyAlias = System.getenv("KEY_ALIAS")
    ?: (project.findProperty("KEY_ALIAS") as? String)
    ?: localProperties.getProperty("release.key.alias")
val resolvedKeyPassword = System.getenv("KEY_PASSWORD")
    ?: (project.findProperty("KEY_PASSWORD") as? String)
    ?: localProperties.getProperty("release.key.password")
    ?: resolvedStorePassword

val isSigningConfigured = resolvedKeystoreFile != null && resolvedKeystoreFile.exists() && !resolvedStorePassword.isNullOrBlank() && !resolvedKeyAlias.isNullOrBlank()

android {
    namespace = "dev.ilamparithi.aournalpp"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.ilamparithi.aournalpp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (resolvedKeystoreFile != null && resolvedKeystoreFile.exists()) {
                storeFile = resolvedKeystoreFile
                storePassword = resolvedStorePassword ?: ""
                keyAlias = resolvedKeyAlias ?: ""
                keyPassword = resolvedKeyPassword ?: ""
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    flavorDimensions += "abi"
    productFlavors {
        create("arm64") {
            dimension = "abi"
            ndk {
                abiFilters.clear()
                abiFilters.add("arm64-v8a")
            }
        }
        create("x86_64") {
            dimension = "abi"
            ndk {
                abiFilters.clear()
                abiFilters.add("x86_64")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (isSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        val jv = JavaVersion.toVersion(libs.versions.javaVersion.get())
        sourceCompatibility = jv
        targetCompatibility = jv
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("arm64") {
            assets.srcDirs("src/arm64/assets", "build/generated/bootstrap-assets/arm64/assets")
        }
        getByName("x86_64") {
            assets.srcDirs("src/x86_64/assets", "build/generated/bootstrap-assets/x86_64/assets")
        }
    }
}

dependencies {
    implementation(project(":x11-core"))
    implementation(project(":runtime-manager"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val bootstrapTasksMap = mapOf(
    "arm64" to "aarch64",
    "x86_64" to "x86_64"
)

bootstrapTasksMap.forEach { (flavorName, archName) ->
    val capitalizedFlavor = flavorName.replaceFirstChar { it.uppercase() }
    val taskName = "generateBootstrap$capitalizedFlavor"
    val flavorOutDir = file("build/generated/bootstrap-assets/$flavorName/assets")
    val flavorOutputFile = File(flavorOutDir, "bootstrap.tar.xz")

    tasks.register<Exec>(taskName) {
        description = "Downloads and builds bootstrap.tar.xz for $flavorName ($archName)"
        group = "build"
        workingDir = rootDir.resolve("scripts")
        outputs.file(flavorOutputFile)
        doFirst {
            flavorOutDir.mkdirs()
        }
        commandLine(
            "python3",
            "build_bootstrap.py",
            "--arch", archName,
            "--output", flavorOutputFile.absolutePath
        )
    }
}

val generateBootstrap = tasks.register("generateBootstrap") {
    description = "Downloads and builds bootstrap.tar.xz for all flavors"
    group = "build"
    dependsOn("generateBootstrapArm64", "generateBootstrapX86_64")
}

androidComponents.onVariants { variant ->
    val flavorName = variant.flavorName ?: return@onVariants
    val capitalizedFlavor = flavorName.replaceFirstChar { it.uppercase() }
    val bootstrapTask = tasks.named("generateBootstrap$capitalizedFlavor")
    val buildTypeName = variant.buildType!!.replaceFirstChar { it.uppercase() }

    val prefixes = listOf(
        "pre${capitalizedFlavor}${buildTypeName}Build",
        "merge${capitalizedFlavor}${buildTypeName}Assets",
        "generate${capitalizedFlavor}${buildTypeName}LintModel",
        "generate${capitalizedFlavor}${buildTypeName}LintVitalModel",
        "generate${capitalizedFlavor}${buildTypeName}LintVitalReportModel"
    )
    tasks.configureEach {
        if (name in prefixes) {
            dependsOn(bootstrapTask)
        }
    }
}