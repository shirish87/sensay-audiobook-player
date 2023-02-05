package buildsrc

import com.android.build.gradle.*
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.internal.DefaultJavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


fun Project.applyCommonPlugins() {
    plugins.apply("kotlin-android")
    plugins.apply("kotlin-kapt")
}

fun Project.applyCommonDependencies(@Suppress("UNUSED_PARAMETER") plugin: Plugin<*>) {
    if (plugin is AppPlugin) {
        plugins.apply("dagger.hilt.android.plugin")
    }

    dependencies {
        if (plugin is AppPlugin) {
            add("implementation", Libraries.AndroidX.window)
            add("implementation", Libraries.Compose.activityCompose)
            add("implementation", Libraries.Compose.navigationCompose)

            // Hilt
            val kapt by configurations.creating
            kapt(Libraries.Hilt.AnnotationProcessor.hiltCompiler)
            add("implementation", Libraries.Hilt.hilt)

            // Mavericks
            add("implementation", Libraries.Mavericks.mavericksCompose)
            add("implementation", Libraries.Mavericks.mavericksHilt)

            // Compose
            add("implementation", Libraries.Compose.foundation)
            add("implementation", Libraries.Compose.foundationLayout)
            add("implementation", Libraries.Compose.constraintLayout)
            add("implementation", Libraries.Compose.composeToolingPreview)
            add("debugImplementation", Libraries.Compose.composeTooling)

            // Material
            add("implementation", Libraries.Material.material)
            add("implementation", Libraries.Compose.material)
            add("implementation", Libraries.Compose.materialIcons)

            // Material3
            add("implementation", Libraries.Compose.material3)
            add("implementation", Libraries.Compose.material3WindowSizeClass)

            // Test Hilt
            val kaptAndroidTest by configurations.creating
            kaptAndroidTest(Libraries.Hilt.AnnotationProcessor.hiltCompiler)
            add("androidTestImplementation", TestLibraries.Hilt.hilt)

            // Test Mavericks
            add("androidTestImplementation", TestLibraries.Mavericks.mavericks)
        }

        add("implementation", Libraries.AndroidX.core)
        add("implementation", Libraries.AndroidX.dataStore)
        add("implementation", Libraries.Coroutines.core)
        add("implementation", Libraries.Coroutines.android)
        add("implementation", Libraries.Logcat.logcat)

        // Test
        add("androidTestImplementation", TestLibraries.AndroidX.core)
        add("androidTestImplementation", TestLibraries.Coroutines.test)

        // AndroidJUnitRunner and JUnit Rules
        add("androidTestImplementation", TestLibraries.AndroidX.runner)
        add("androidTestImplementation", TestLibraries.AndroidX.rules)
    }
}

fun KotlinCompile.configure(extension: AutoConfigBuildPluginExtension) {
    kotlinOptions {
        jvmTarget = extension.javaVersion.get()
        allWarningsAsErrors = extension.warningsAsErrors.getOrElse(true) != false

        // Enable experimental coroutines APIs, including Flow
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            // "-opt-in=kotlin.Experimental",
        )
    }
}

fun JavaCompile.configure(extension: AutoConfigBuildPluginExtension) {
    sourceCompatibility = extension.javaVersion.get()
    targetCompatibility = extension.javaVersion.get()
}

fun DefaultJavaPluginExtension.configure(
    @Suppress("UNUSED_PARAMETER") plugin: Plugin<*>,
    extension: AutoConfigBuildPluginExtension,
) {
    val javaVersion = JavaVersion.valueOf("VERSION_${extension.javaVersion.get()}")
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

fun AndroidExtensionsExtension.configure(@Suppress("UNUSED_PARAMETER") plugin: Plugin<*>) {
    isExperimental = true
}

fun Project.applyLintConfig(@Suppress("UNUSED_PARAMETER") plugin: Plugin<*>) {
    val ktlint by configurations.creating

    dependencies {
        ktlint(Libraries.KtLint.ktlint)
    }

    tasks.register<JavaExec>("ktlint") {
        group = "verification"
        description = "Check Kotlin code style."
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        args("--android", "src/**/*.kt")
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    }

    tasks.named("check") {
        dependsOn(ktlint)
    }

    tasks.register<JavaExec>("ktlintFormat") {
        group = "formatting"
        description = "Fix Kotlin code style deviations."
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        args("--android", "-F", "src/**/*.kt")
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    }
}

@Suppress("UnstableApiUsage")
fun BaseExtension.configure(
    plugin: Plugin<*>,
    extension: AutoConfigBuildPluginExtension,
) {
    compileSdkVersion(Versions.compileSdk)
    buildToolsVersion(Versions.buildTools)

    defaultConfig {
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk

        if (plugin is AppPlugin) {
            applicationId = AppConfig.applicationId
            versionName = AppConfig.versionName
            versionCode = AppConfig.versionCode
        }

        vectorDrawables {
            useSupportLibrary = false
        }

        testInstrumentationRunnerArguments += mapOf(
            "disableAnalytics" to "true",
        )
    }

    compileOptions {
        val javaVersion = JavaVersion.valueOf("VERSION_${extension.javaVersion.get()}")
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    sourceSets {
        listOf("main", "androidTest", "test").forEach { srcType ->
            getByName(srcType).java.apply {
                setSrcDirs(srcDirs.plus("src/${srcType}/kotlin"))
            }
        }
    }

    lintOptions {
        disable += "ObsoleteLintCustomCheck"
    }

    when (this) {
        is AppExtension -> {
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true

                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
            }

            buildFeatures.apply {
                compose = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = Libraries.Compose.Compiler.version
            }
            packagingOptions {
                resources {
                    excludes.add("/META-INF/{AL2.0,LGPL2.1,LICENSE*}")
                }
            }
        }
        is LibraryExtension -> {
            // apply config options for library modules
        }
        else -> {
            // apply config options for dynamic modules
        }
    }
}

interface AutoConfigBuildPluginExtension {
    val javaVersion: Property<String>
    val disableLint: Property<Boolean>
    val warningsAsErrors: Property<Boolean>
}

class AutoConfigBuildPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create<AutoConfigBuildPluginExtension>("autoConfigBuild")

        project.subprojects {
            applyCommonPlugins()

            plugins.whenPluginAdded {
                if (this is KotlinAndroidPluginWrapper) {
                    extensions.findByType<AndroidExtensionsExtension>()?.configure(this)
                }

                if (this is AppPlugin || this is LibraryPlugin) {
                    // Only apply below code to App (com.android.application)
                    // or Library (com.android.library) projects

                    applyCommonDependencies(this)
                }
            }
        }

        project.afterEvaluate {
            // Sections that require AutoConfigBuildPluginExtension config values

            subprojects {
                tasks.withType<KotlinCompile>().configureEach { configure(extension) }
                tasks.withType<JavaCompile>().configureEach { configure(extension) }

                plugins.whenPluginAdded {
                    if (this is AppPlugin || this is LibraryPlugin) {
                        // Only apply below code to App (com.android.application)
                        // or Library (com.android.library) projects

                        if (!extension.disableLint.getOrElse(false)) {
                            applyLintConfig(this)
                        }

                        (extensions.findByName("android") as BaseExtension?)
                            ?.configure(this, extension)

                        (extensions.findByName("java") as DefaultJavaPluginExtension?)
                            ?.configure(this, extension)
                    }
                }
            }
        }
    }
}
