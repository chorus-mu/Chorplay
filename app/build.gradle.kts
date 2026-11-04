import com.android.build.api.variant.BuildConfigField
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.baselineprofile")
    alias(libs.plugins.room)
}

val localProperties =
    Properties().apply {
        rootDir
            .resolve("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use { load(it) }
    }

fun secretValue(
    property: String,
    localKey: String,
    environment: String,
): String =
    (project.findProperty(property) as String?)
        ?: localProperties.getProperty(localKey)
        ?: System.getenv(environment)
        ?: ""

fun translatedLocaleTags(): List<String> =
    file("src/main/res")
        .listFiles()
        .orEmpty()
        .filter { it.isDirectory && it.resolve("strings.xml").exists() }
        .mapNotNull { dir ->
            val qualifier = dir.name.removePrefix("values-")
            when {
                qualifier == dir.name -> null
                qualifier.startsWith("b+") -> qualifier.removePrefix("b+").replace('+', '-')
                Regex("[a-z]{2,3}(-r[A-Z]{2})?").matches(qualifier) -> qualifier.replace("-r", "-")
                else -> null
            }
        }.map { if (it == "in" || it.startsWith("in-")) "id" + it.removePrefix("in") else it }
        .sorted()

android {
    namespace = "io.github.pushkar.chorplay"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.pushkar.chorplay"
        minSdk = 26
        targetSdk = 36
        versionCode = 19
        versionName = "3.0.0"

        buildConfigField("int", "NIGHTLY_RUN", "0")
        buildConfigField("String", "APP_LANGUAGE_TAGS", "\"${(listOf("en") + translatedLocaleTags()).joinToString(",")}\"")
        buildConfigField("String", "LASTFM_API_KEY", "\"${secretValue("lastfmApiKey", "lastfm.apiKey", "LASTFM_API_KEY")}\"")
        buildConfigField("String", "LASTFM_API_SECRET", "\"${secretValue("lastfmApiSecret", "lastfm.apiSecret", "LASTFM_API_SECRET")}\"")

        testInstrumentationRunner = "io.github.pushkar.chorplay.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("github") {
            dimension = "version"
            isDefault = true
            buildConfigField("Boolean", "UPDATER_ENABLED", "true")
        }
        create("foss") {
            dimension = "version"
            buildConfigField("Boolean", "UPDATER_ENABLED", "false")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = rootDir.resolve("release.keystore")
            storePassword = (project.findProperty("storePassword") as? String)
                ?: localProperties.getProperty("storePassword")
                ?: System.getenv("STORE_PASSWORD")
                ?: ""
            keyAlias = (project.findProperty("keyAlias") as? String)
                ?: localProperties.getProperty("keyAlias")
                ?: System.getenv("KEY_ALIAS")
                ?: ""
            keyPassword = (project.findProperty("keyPassword") as? String)
                ?: localProperties.getProperty("keyPassword")
                ?: System.getenv("KEY_PASSWORD")
                ?: ""
        }

        create("nightly") {
            storeFile = rootDir.resolve("nightly.keystore")
            storePassword = System.getenv("NIGHTLY_STORE_PASSWORD") ?: localProperties.getProperty("nightlyStorePassword") ?: ""
            keyAlias = System.getenv("NIGHTLY_KEY_ALIAS") ?: localProperties.getProperty("nightlyKeyAlias") ?: ""
            keyPassword = System.getenv("NIGHTLY_KEY_PASSWORD") ?: localProperties.getProperty("nightlyKeyPassword") ?: ""
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "UPDATE_CHANNEL", "\"stable\"")
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            buildConfigField("String", "UPDATE_CHANNEL", "\"stable\"")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            val releaseKeystore =
                try {
                    signingConfigs.getByName("release").storeFile
                } catch (e: Exception) {
                    null
                }
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseKeystore?.exists() == true && !releaseConfig.storePassword.isNullOrBlank() && !releaseConfig.keyAlias.isNullOrBlank()) {
                signingConfig = releaseConfig
                println("Using RELEASE signing config with keystore: ${releaseKeystore.absolutePath}")
            } else {
                signingConfig = null
                println("WARNING: Release keystore or credentials not configured. Building UNSIGNED release APK.")
            }
        }

        create("nightly") {
            initWith(getByName("release"))
            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-nightly"
            matchingFallbacks += "release"
            buildConfigField("String", "UPDATE_CHANNEL", "\"nightly\"")
            val nightlyKey = signingConfigs.getByName("nightly")
            signingConfig = if (nightlyKey.storeFile?.exists() == true) nightlyKey else signingConfigs.getByName("debug")
        }
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes +=
                listOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "/META-INF/INDEX.LIST",
                    "/META-INF/DEPENDENCIES",
                    "/META-INF/*.version",
                )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "org.conscrypt", module = "conscrypt-android")
}

tasks.withType<Test>().configureEach {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

baselineProfile {
    mergeIntoMain = true
}

androidComponents {
    onVariants(selector().withBuildType("nightly")) { variant ->
        val run = providers.environmentVariable("GITHUB_RUN_NUMBER").map(String::toInt)
        val sha = providers.environmentVariable("GITHUB_SHA").map { it.take(7) }
        val base = providers.gradleProperty("chorplay.nightlyBaseVersion")
        variant.outputs.forEach { output ->
            val localCode = output.versionCode.get()
            val localName = output.versionName.get()
            output.versionCode.set(run.orElse(localCode))
            output.versionName.set(
                base
                    .zip(run) { name, number -> "$name-nightly.$number" }
                    .zip(sha) { name, commit -> "$name+$commit" }
                    .orElse(localName),
            )
        }
        variant.buildConfigFields?.put(
            "NIGHTLY_RUN",
            run
                .map { BuildConfigField("int", it.toString(), null) }
                .orElse(BuildConfigField("int", "0", null)),
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

composeCompiler {
    if (project.findProperty("composeCompilerReports") == "true") {
        val reportsDir = layout.buildDirectory.dir("compose_compiler")
        reportsDestination = reportsDir
        metricsDestination = reportsDir
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.core)
    implementation(libs.androidx.material3.adaptive.layout)
    implementation(libs.androidx.material3.adaptive.navigation)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.coil.compose)
    implementation(libs.compose.reorderable)
    implementation(libs.coil.video)
    implementation(libs.coil.network.okhttp)
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.server.core) {
        exclude(group = "org.fusesource.jansi", module = "jansi")
    }
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)

    implementation(libs.conscrypt.android)

    implementation(libs.bouncycastle.bcprov)
    implementation(libs.kotlin.bip39)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.muxer)
    implementation(libs.androidx.media3.inspector)
    implementation(libs.androidx.media)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.graphics.shapes)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.brotli)
    implementation(libs.re2j)

    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tracing)
    baselineProfile(project(":benchmark"))

    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)

    testImplementation(libs.junit)
    testImplementation(libs.conscrypt.openjdk.uber)
    testImplementation(libs.kxml2)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.icu4j)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.glance.appwidget.testing)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.android.compiler)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

ksp {
    arg("dagger.fastInit", "enabled")
}

hilt {
    enableAggregatingTask = true
}
