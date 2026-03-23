plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sqldelight)
}

val appVersionName: String = providers.gradleProperty("app.versionName").get()

kotlin {
    // Set language and API version for all targets to help IntelliJ recognize Kotlin 2.0+ APIs
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }

    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // Ktor engine for Android
            implementation(libs.ktor.client.okhttp)

            // Coroutines Android
            implementation(libs.kotlinx.coroutines.android)

            // SQLDelight Android driver
            implementation(libs.sqldelight.android)
        }

        iosMain.dependencies {
            // Ktor engine for iOS
            implementation(libs.ktor.client.darwin)

            // SQLDelight Native driver
            implementation(libs.sqldelight.native)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // DI - kotlin-inject runtime
            implementation(libs.kotlinInject.runtime)

            // Networking - Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.ktor.client.logging)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Logging
            implementation(libs.kermit)

            // Settings/Preferences
            implementation(libs.multiplatformSettings)
            implementation(libs.multiplatformSettings.noArg)
            implementation(libs.multiplatformSettings.coroutines)

            // Navigation
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenModel)
            implementation(libs.voyager.transitions)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            // Date/Time
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)

            // Kotest
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.property)

            // Coroutines testing
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// KSP configuration for kotlin-inject
dependencies {
    // kotlin-inject compiler for each target
    add("kspAndroid", libs.kotlinInject.compiler)
    add("kspIosArm64", libs.kotlinInject.compiler)
    add("kspIosSimulatorArm64", libs.kotlinInject.compiler)
}

android {
    namespace = "cloud.osasoft.dartzvibe"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()

    defaultConfig {
        applicationId = "cloud.osasoft.dartzvibe"
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        targetSdk = libs.versions.android.targetSdk
            .get()
            .toInt()
        versionCode = providers.gradleProperty("app.versionCode").get().toInt()
        versionName = appVersionName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

// Generate BuildInfo.kt with version from gradle.properties
val generateBuildInfo by tasks.registering(Sync::class) {
    from(
        resources.text.fromString(
            """
            |package cloud.osasoft.dartzvibe.config
            |
            |object BuildInfo {
            |    const val VERSION_NAME = "$appVersionName"
            |}
            |
            """.trimMargin(),
        ),
    ) {
        rename { "BuildInfo.kt" }
        into("cloud/osasoft/dartzvibe/config")
    }
    into(layout.buildDirectory.dir("generated/buildinfo"))
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(generateBuildInfo.map { layout.buildDirectory.dir("generated/buildinfo") })
}

// SQLDelight configuration
sqldelight {
    databases {
        create("DartzVibeDatabase") {
            packageName.set("cloud.osasoft.dartzvibe.data.local")
            verifyMigrations.set(!System.getProperty("os.name").lowercase().contains("win"))
        }
    }
}
