import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Local only. Environment variables take precedence; never print these values.
val releaseSigningFile = layout.projectDirectory.file("gelatostock-release-signing.properties").asFile
val releaseSigningProperties = Properties().apply {
    if (releaseSigningFile.isFile) releaseSigningFile.inputStream().use { load(it) }
}
fun signingValue(name: String): String? = providers.environmentVariable("GELATOSTOCK_$name")
    .orNull ?: releaseSigningProperties.getProperty(name)

val releaseStorePath = signingValue("STORE_FILE")
val releaseStorePassword = signingValue("STORE_PASSWORD")
val releaseKeyAlias = signingValue("KEY_ALIAS")
val releaseKeyPassword = signingValue("KEY_PASSWORD")
val releaseSigningComplete = listOf(releaseStorePath, releaseStorePassword, releaseKeyAlias,
    releaseKeyPassword).all { !it.isNullOrBlank() }
val releaseStore = releaseStorePath?.takeIf { it.isNotBlank() }?.let { file(it).canonicalFile }
val releaseStoreExternal = releaseStore != null &&
    !releaseStore.toPath().startsWith(rootProject.projectDir.canonicalFile.toPath())
val releaseSigningReady = releaseSigningComplete && releaseStoreExternal && releaseStore?.isFile == true

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    group = "verification"
    description = "Requires a complete local signing configuration and an external keystore."
    inputs.property("signingReady", releaseSigningReady)
    doLast {
        check(inputs.properties["signingReady"] == true) {
            "Assinatura release indisponivel: configure STORE_FILE, STORE_PASSWORD, KEY_ALIAS e " +
                "KEY_PASSWORD via GELATOSTOCK_* ou app/gelatostock-release-signing.properties. " +
                "A keystore deve existir fora do projeto. Consulte README.md."
        }
    }
}

android {
    namespace = "com.flstudios.a2lopes"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.flstudios.gelatostock"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("definitiveRelease") {
                storeFile = releaseStore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("definitiveRelease")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")
}

// Compilation, R8 and lint remain available without a key; distributable packaging does not.
tasks.configureEach {
    if (name in setOf("packageRelease", "assembleRelease", "packageReleaseBundle", "signReleaseBundle", "bundleRelease")) {
        dependsOn(validateReleaseSigning)
    }
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
