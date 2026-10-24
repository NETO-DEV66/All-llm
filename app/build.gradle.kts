import java.util.Properties
import org.gradle.api.tasks.Sync

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val testSecrets = Properties().apply {
    val secretsFile = rootProject.file("test-secrets.properties")
    if (secretsFile.isFile) secretsFile.inputStream().use(::load)
}
val playBuild = providers.gradleProperty("playBuild").orNull?.toBoolean() == true ||
    providers.gradleProperty("playFeasibility").orNull?.toBoolean() == true
val privacyPolicyUrl = providers.gradleProperty("privacyPolicyUrl").orNull
    ?: "https://github.com/NETO-DEV66/All-llm/blob/main/PRIVACY.md"
val uploadStorePath = providers.environmentVariable("ALLLM_UPLOAD_STORE_FILE").orNull
val uploadStorePassword = providers.environmentVariable("ALLLM_UPLOAD_STORE_PASSWORD").orNull
val uploadKeyAlias = providers.environmentVariable("ALLLM_UPLOAD_KEY_ALIAS").orNull
val uploadKeyPassword = providers.environmentVariable("ALLLM_UPLOAD_KEY_PASSWORD").orNull
val hasUploadSigning = listOf(
    uploadStorePath,
    uploadStorePassword,
    uploadKeyAlias,
    uploadKeyPassword,
).all { !it.isNullOrBlank() }
val appVersionName = providers.gradleProperty("appVersionName").orNull ?: "3.9.2"
val runtimeReleaseBaseUrl =
    "https://github.com/NETO-DEV66/All-llm/releases/download/v$appVersionName"
val appUpdateManifestUrl =
    "https://github.com/NETO-DEV66/All-llm/releases/latest/download/alllm-update.json"
val runtimeBundleDir = rootProject.layout.projectDirectory.dir("dist/runtime-bundles")
val generatedRuntimeAssets = layout.buildDirectory.dir("generated/runtime-assets")

val prepareBundledAgentAssets = tasks.register<Sync>("prepareBundledAgentAssets") {
    // The release workflow resolves the newest verified runtime assets before every build.
    from(runtimeBundleDir) {
        include("manifest.json")
    }
    into(generatedRuntimeAssets.map { it.dir("shared/runtime") })
}

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.netodev66.alllm"
    compileSdk = 36
    // F-Droid's r26b recipe installs 26.1.10909125. Keep AGP from selecting
    // its newer default NDK; local developers may override this explicitly.
    ndkVersion = providers.gradleProperty("mhNdkVersion").orNull ?: "26.1.10909125"

    signingConfigs {
        if (hasUploadSigning) {
            create("upload") {
                storeFile = rootProject.file(checkNotNull(uploadStorePath))
                storePassword = checkNotNull(uploadStorePassword)
                keyAlias = checkNotNull(uploadKeyAlias)
                keyPassword = checkNotNull(uploadKeyPassword)
            }
        }
    }

    defaultConfig {
        applicationId = "com.netodev66.alllm"
        minSdk = 28
        // The direct APK retains the proven target-28 PRoot execution path. The
        // Play build targets current Android while its runtime path is validated.
        targetSdk = if (playBuild) 36 else 28
        // Keep literal defaults so F-Droid's static manifest parser can detect
        // the tagged release. Gradle properties may still override Play builds.
        versionCode = 66
        versionName = appVersionName
        providers.gradleProperty("appVersionCode").orNull?.toIntOrNull()?.let { versionCode = it }

        ndk.abiFilters += "arm64-v8a"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("boolean", "IS_PLAY_BUILD", playBuild.toString())
        buildConfigField("String", "PRIVACY_POLICY_URL", buildConfigString(privacyPolicyUrl))

        buildConfigField(
            "String",
            "TEST_OPENROUTER_API_KEY",
            "\"\"",
        )
    }

    flavorDimensions += "runtimeDelivery"
    productFlavors {
        create("online") {
            dimension = "runtimeDelivery"
            buildConfigField("boolean", "OFFLINE_RUNTIME_BUNDLES", "false")
            buildConfigField("String", "RUNTIME_RELEASE_BASE_URL", buildConfigString(runtimeReleaseBaseUrl))
            buildConfigField("String", "APP_UPDATE_MANIFEST_URL", buildConfigString(appUpdateManifestUrl))
            buildConfigField("String", "APP_VARIANT", "\"online\"")
        }
    }

    sourceSets.getByName("main").assets.srcDir(generatedRuntimeAssets.map { it.dir("shared") })

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "TEST_OPENROUTER_API_KEY",
                buildConfigString(testSecrets.getProperty("openrouter.apiKey", "")),
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasUploadSigning) {
                signingConfig = signingConfigs.getByName("upload")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    val enableCmake = providers.gradleProperty("mh.buildNative").orNull?.toBoolean()
        ?: (System.getProperty("os.arch") != "aarch64" && System.getProperty("os.arch") != "arm64")

    if (enableCmake) {
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    } else {
        sourceSets.getByName("main").jniLibs.srcDir("src/main/prebuiltJniLibs")
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    packaging.jniLibs.useLegacyPackaging = true
    androidResources.noCompress += "zst"
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }
    .configureEach { dependsOn(prepareBundledAgentAssets) }

tasks.matching { it.name.contains("lint", ignoreCase = true) }
    .configureEach { dependsOn(prepareBundledAgentAssets) }

tasks.register("playReadinessCheck") {
    group = "verification"
    description = "Checks configuration required before uploading a All-LLM Play bundle."
    doLast {
        check(playBuild) { "Run with -PplayBuild=true." }
        check(privacyPolicyUrl.startsWith("https://")) {
            "privacyPolicyUrl must be a public HTTPS URL."
        }
        check(hasUploadSigning) {
            "Set ALLLM_UPLOAD_STORE_FILE, ALLLM_UPLOAD_STORE_PASSWORD, ALLLM_UPLOAD_KEY_ALIAS, and ALLLM_UPLOAD_KEY_PASSWORD."
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("com.github.luben:zstd-jni:1.5.6-9@aar")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250107")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
