import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.termux.x11"
    compileSdk = 34
    ndkVersion = "25.1.8937393"

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }

        buildConfigField("String", "VERSION_NAME", "\"1.03.01\"")
        buildConfigField("String", "COMMIT", "\"termux-x11\"")
        buildConfigField("String", "APPLICATION_ID", "\"dev.ilamparithi.aournalpp\"")

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17 -fexceptions -frtti")
                arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DPython3_EXECUTABLE=/usr/bin/python3"
                )
            }
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs(
                "src/main/java",
                "../submodules/termux-x11/lorie/src/main/java",
                "../submodules/termux-x11/shell-loader/stub/src/main/java",
                layout.buildDirectory.dir("generated/java").get().asFile.absolutePath
            )
            aidl.srcDirs(
                "../submodules/termux-x11/lorie/src/main/aidl"
            )
            res.srcDirs(
                "src/main/res",
                "../submodules/termux-x11/lorie/src/main/res",
                layout.buildDirectory.dir("generated/templateRes").get().asFile.absolutePath
            )
            jniLibs.srcDirs(
                "src/main/jniLibs",
                "../submodules/termux-x11/lorie/src/main/jniLibs"
            )
        }
    }

    externalNativeBuild {
        cmake {
            path("../submodules/termux-x11/lorie/src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
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
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val generatePrefs = tasks.register("generatePrefs") {
    val prefsXmlFile = file("../submodules/termux-x11/lorie/src/main/res/xml/preferences.xml")
    val outputDir = layout.buildDirectory.dir("generated/java/com/termux/x11").get().asFile
    val outputFile = File(outputDir, "Prefs.java")
    inputs.file(prefsXmlFile)
    outputs.file(outputFile)

    doLast {
        outputDir.mkdirs()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(prefsXmlFile)
        val nodeList = doc.getElementsByTagName("*")
        data class PrefItem(val type: String, val key: String, val defaultVal: String, val entries: String? = null, val values: String? = null)
        val preferences = mutableListOf<PrefItem>()

        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            val key = node.attributes?.getNamedItem("app:key")?.nodeValue ?: continue
            val defaultVal = node.attributes?.getNamedItem("app:defaultValue")?.nodeValue ?: ""
            when (node.nodeName) {
                "EditTextPreference" -> {
                    if (key != "extra_keys_config") {
                        preferences.add(PrefItem("String", key, defaultVal))
                    }
                }
                "SeekBarPreference" -> preferences.add(PrefItem("Int", key, defaultVal))
                "ListPreference" -> {
                    val entries = node.attributes?.getNamedItem("app:entries")?.nodeValue?.removePrefix("@array/") ?: ""
                    val values = node.attributes?.getNamedItem("app:entryValues")?.nodeValue?.removePrefix("@array/") ?: ""
                    preferences.add(PrefItem("List", key, defaultVal, entries, values))
                }
                "SwitchPreferenceCompat" -> preferences.add(PrefItem("Boolean", key, defaultVal))
            }
        }

        val sb = StringBuilder()
        sb.append("package com.termux.x11;\n")
        sb.append("import java.util.HashMap;\n")
        sb.append("import android.content.Context;\n")
        sb.append("import android.content.SharedPreferences;\n")
        sb.append("import com.termux.x11.utils.TermuxX11ExtraKeys;\n\n")
        sb.append("public class Prefs extends LoriePreferences.PrefsProto {\n")
        for (p in preferences) {
            when (p.type) {
                "Int", "Boolean" -> sb.append("  public final ${p.type}Preference ${p.key} = new ${p.type}Preference(\"${p.key}\", ${p.defaultVal});\n")
                "String" -> sb.append("  public final StringPreference ${p.key} = new StringPreference(\"${p.key}\", \"${p.defaultVal}\");\n")
                "List" -> sb.append("  public final ${p.type}Preference ${p.key} = new ${p.type}Preference(\"${p.key}\", \"${p.defaultVal}\", R.array.${p.entries}, R.array.${p.values});\n")
            }
        }
        sb.append("  public final StringPreference extra_keys_config = new StringPreference(\"extra_keys_config\", TermuxX11ExtraKeys.DEFAULT_IVALUE_EXTRA_KEYS);\n")
        sb.append("  public final HashMap<String, Preference> keys = new HashMap<>() {{\n")
        for (p in preferences) {
            sb.append("    put(\"${p.key}\", ${p.key});\n")
        }
        sb.append("    put(\"extra_keys_config\", extra_keys_config);\n")
        sb.append("  }};\n\n")
        sb.append("  public Prefs(Context ctx) {\n")
        sb.append("    super(ctx);\n")
        sb.append("    for (SharedPreferences store : new SharedPreferences[]{ builtInDisplayPreferences, secondaryDisplayPreferences }) {\n")
        sb.append("      Object legacyKeepScreenOn = store.getAll().get(\"keepScreenOn\");\n")
        sb.append("      if (legacyKeepScreenOn instanceof Boolean && !store.contains(\"screenIdleTimeout\"))\n")
        sb.append("        store.edit().putString(\"screenIdleTimeout\", ((Boolean) legacyKeepScreenOn) ? \"never\" : \"system\").commit();\n")
        sb.append("    }\n")
        sb.append("  }\n")
        sb.append("}\n")

        outputFile.writeText(sb.toString())
    }
}

val generateShortcuts = tasks.register("generateShortcuts") {
    val templateFile = file("../submodules/termux-x11/lorie/src/main/templates/xml/shortcuts.xml")
    val outputDir = layout.buildDirectory.dir("generated/templateRes/xml").get().asFile
    val outputFile = File(outputDir, "lorie_shortcuts.xml")
    inputs.file(templateFile)
    outputs.file(outputFile)

    doLast {
        outputDir.mkdirs()
        val text = templateFile.readText().replace("@@APPLICATION_ID@@", "dev.ilamparithi.aournalpp")
        outputFile.writeText(text)
    }
}

tasks.named("preBuild") {
    dependsOn(generatePrefs)
    dependsOn(generateShortcuts)
}
