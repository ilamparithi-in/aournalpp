import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localPropsFile = rootDir.resolve("local.properties")
val localProperties = Properties().apply {
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

// -----------------------------------------------------------------------------
// Dynamic Versioning System (git describe --tags --always --dirty)
// -----------------------------------------------------------------------------
val fallbackVersionName = "${libs.versions.appVersionMajor.get()}.${libs.versions.appVersionMinor.get()}.${libs.versions.appVersionPatch.get()}"

val gitVersionName: String = try {
    providers.exec {
        commandLine("git", "describe", "--tags", "--always", "--dirty")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().removePrefix("v").ifEmpty { fallbackVersionName }
} catch (e: Exception) {
    fallbackVersionName
}

val baseVersionCode: Int = try {
    providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
    1
}

// -----------------------------------------------------------------------------
// Multi-Keystore Signing Resolution (Release & Beta)
// -----------------------------------------------------------------------------
val releaseKeystoreFilePath = System.getenv("RELEASE_KEYSTORE_FILE")
    ?: System.getenv("KEYSTORE_FILE")
    ?: (project.findProperty("RELEASE_KEYSTORE_FILE") as? String)
    ?: (project.findProperty("KEYSTORE_FILE") as? String)
    ?: localProperties.getProperty("release.keystore.path")
val releaseKeystoreFile = releaseKeystoreFilePath?.let { file(it) }
val releaseStorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
    ?: System.getenv("KEYSTORE_PASSWORD")
    ?: (project.findProperty("RELEASE_KEYSTORE_PASSWORD") as? String)
    ?: (project.findProperty("KEYSTORE_PASSWORD") as? String)
    ?: localProperties.getProperty("release.keystore.password")
val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
    ?: System.getenv("KEY_ALIAS")
    ?: (project.findProperty("RELEASE_KEY_ALIAS") as? String)
    ?: (project.findProperty("KEY_ALIAS") as? String)
    ?: localProperties.getProperty("release.key.alias")
val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")
    ?: System.getenv("KEY_PASSWORD")
    ?: (project.findProperty("RELEASE_KEY_PASSWORD") as? String)
    ?: (project.findProperty("KEY_PASSWORD") as? String)
    ?: localProperties.getProperty("release.key.password")
    ?: releaseStorePassword
val isReleaseSigningConfigured = releaseKeystoreFile != null && releaseKeystoreFile.exists() && !releaseStorePassword.isNullOrBlank() && !releaseKeyAlias.isNullOrBlank()

val betaKeystoreFilePath = System.getenv("BETA_KEYSTORE_FILE")
    ?: (project.findProperty("BETA_KEYSTORE_FILE") as? String)
    ?: localProperties.getProperty("beta.keystore.path")
val betaKeystoreFile = betaKeystoreFilePath?.let { file(it) }
val betaStorePassword = System.getenv("BETA_KEYSTORE_PASSWORD")
    ?: (project.findProperty("BETA_KEYSTORE_PASSWORD") as? String)
    ?: localProperties.getProperty("beta.keystore.password")
val betaKeyAlias = System.getenv("BETA_KEY_ALIAS")
    ?: (project.findProperty("BETA_KEY_ALIAS") as? String)
    ?: localProperties.getProperty("beta.key.alias")
val betaKeyPassword = System.getenv("BETA_KEY_PASSWORD")
    ?: (project.findProperty("BETA_KEY_PASSWORD") as? String)
    ?: localProperties.getProperty("beta.key.password")
    ?: betaStorePassword
val isBetaSigningConfigured = betaKeystoreFile != null && betaKeystoreFile.exists() && !betaStorePassword.isNullOrBlank() && !betaKeyAlias.isNullOrBlank()

android {
    namespace = "dev.ilamparithi.aournalpp"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.ilamparithi.aournalpp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = baseVersionCode
        versionName = gitVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (isReleaseSigningConfigured) {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword ?: ""
                keyAlias = releaseKeyAlias ?: ""
                keyPassword = releaseKeyPassword ?: ""
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
        create("beta") {
            if (isBetaSigningConfigured) {
                storeFile = betaKeystoreFile
                storePassword = betaStorePassword ?: ""
                keyAlias = betaKeyAlias ?: ""
                keyPassword = betaKeyPassword ?: ""
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
            versionCode = baseVersionCode * 10 + 2
            ndk {
                abiFilters.clear()
                abiFilters.add("arm64-v8a")
            }
        }
        create("x86_64") {
            dimension = "abi"
            versionCode = baseVersionCode * 10 + 4
            ndk {
                abiFilters.clear()
                abiFilters.add("x86_64")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isPseudoLocalesEnabled = true
        }
        create("beta") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            if (isBetaSigningConfigured) {
                signingConfig = signingConfigs.getByName("beta")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (isReleaseSigningConfigured) {
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
    androidResources {
        noCompress += listOf("xz", "tar.xz")
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }

    sourceSets {
        getByName("arm64") {
            assets.directories += listOf("src/arm64/assets", "build/generated/bootstrap-assets/arm64/assets")
            jniLibs.directories += listOf("src/arm64/jniLibs", "build/generated/bootstrap-assets/arm64/jniLibs")
        }
        getByName("x86_64") {
            assets.directories += listOf("src/x86_64/assets", "build/generated/bootstrap-assets/x86_64/assets")
            jniLibs.directories += listOf("src/x86_64/jniLibs", "build/generated/bootstrap-assets/x86_64/jniLibs")
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

    // Security & WorkManager
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Network & Storage Protocols
    implementation(libs.okhttp)
    implementation(libs.sshj)
    implementation(libs.smbj)
    implementation(libs.commons.net)
    implementation(libs.slf4j.android)
    implementation(libs.androidx.browser)

    // CameraX & QR Code Scanner
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

configurations.all {
    resolutionStrategy {
        force("org.bouncycastle:bcprov-jdk18on:1.80")
        force("org.bouncycastle:bcpkix-jdk18on:1.80")
        force("org.bouncycastle:bcutil-jdk18on:1.80")
    }
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
    val flavorJniDir = file("build/generated/bootstrap-assets/$flavorName/jniLibs")
    val flavorOutputFile = File(flavorOutDir, "bootstrap.tar.xz")

    tasks.register<Exec>(taskName) {
        description = "Downloads and builds bootstrap.tar.xz and jniLibs for $flavorName ($archName)"
        group = "build"
        workingDir = rootDir.resolve("scripts")
        inputs.dir(rootDir.resolve("scripts"))
        outputs.file(flavorOutputFile)
        outputs.dir(flavorJniDir)
        doFirst {
            flavorOutDir.mkdirs()
            flavorJniDir.mkdirs()
        }
        commandLine(
            "python3",
            "build_bootstrap.py",
            "--arch", archName,
            "--output", flavorOutputFile.absolutePath,
            "--jnilibs-dir", flavorJniDir.absolutePath
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
        "merge${capitalizedFlavor}${buildTypeName}JniLibFolders",
        "merge${capitalizedFlavor}${buildTypeName}NativeLibs",
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