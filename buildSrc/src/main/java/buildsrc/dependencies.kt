package buildsrc

object Versions {
    // Build tools and SDK
    const val buildTools = "33.0.0"
    const val compileSdk = 33
    const val minSdk = 30
    const val targetSdk = 33
}

object Libraries {
    @JvmStatic
    val buildscriptPlugins: Array<String> = arrayOf(
        Gradle.gradlePlugin,
        Kotlin.gradlePlugin,
        Hilt.gradlePlugin,
    )

    object Gradle {
        private const val version = "7.3.1"

        const val gradlePlugin = "com.android.tools.build:gradle:$version"
    }

    object Kotlin {
        private const val version = "1.7.22"

        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    }

    object Coroutines {
        const val version = "1.6.4"
        private const val guavaVersion = "1.6.4"

        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"

        const val guava = "org.jetbrains.kotlinx:kotlinx-coroutines-guava:$guavaVersion"
    }

    object Hilt {
        const val version = "2.44.2"

        const val gradlePlugin = "com.google.dagger:hilt-android-gradle-plugin:$version"
        const val hilt = "com.google.dagger:hilt-android:$version"

        object AnnotationProcessor {
            const val hiltCompiler = "com.google.dagger:hilt-android-compiler:$version"
        }
    }

    object AndroidX {
        private const val version = "1.9.0"
        private const val dataStoreVersion = "1.0.0"
        private const val windowVersion = "1.0.0"

        const val core = "androidx.core:core-ktx:$version"
        const val dataStore = "androidx.datastore:datastore-preferences:$dataStoreVersion"
        const val window = "androidx.window:window:$windowVersion"
    }

    object Material {
        private const val version = "1.8.0-beta01"

        const val material = "com.google.android.material:material:$version"
    }

    object Compose {
        private const val version = "1.3.2"
        private const val material3ComposeVersion = "1.0.1"

        const val activityCompose = "androidx.activity:activity-compose:1.6.1"
        const val navigationCompose = "androidx.navigation:navigation-compose:2.5.3"

        const val foundation = "androidx.compose.foundation:foundation:1.3.1"
        const val foundationLayout = "androidx.compose.foundation:foundation-layout:1.3.1"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout-compose:1.0.1"


        const val material3 = "androidx.compose.material3:material3:$material3ComposeVersion"
        const val material3WindowSizeClass = "androidx.compose.material3:material3-window-size-class:$material3ComposeVersion"

        const val material = "androidx.compose.material:material:1.3.1"
        const val materialIcons = "androidx.compose.material:material-icons-core:1.3.1"
        const val materialIconsExtended = "androidx.compose.material:material-icons-extended:1.3.1"

        const val composeTooling = "androidx.compose.ui:ui-tooling:$version"
        const val composeToolingPreview = "androidx.compose.ui:ui-tooling-preview:$version"

        object Compiler {
            const val version = "1.3.2"
        }
    }

    object Mavericks {
        const val version = "3.0.1"

        const val mavericksCompose = "com.airbnb.android:mavericks-compose:$version"
        const val mavericksHilt = "com.airbnb.android:mavericks-hilt:$version"
    }

    object Accompanist {
        private const val version = "0.28.0"

        const val accompanistNavigation = "com.google.accompanist:accompanist-navigation-animation:$version"
        const val accompanistFlowLayout = "com.google.accompanist:accompanist-flowlayout:$version"
        const val systemUiController = "com.google.accompanist:accompanist-systemuicontroller:$version"
    }

    object Room {
        private const val version = "2.4.3"

        const val room = "androidx.room:room-ktx:$version"
        const val roomPaging = "androidx.room:room-paging:$version"
        const val roomRuntime = "androidx.room:room-runtime:$version"

        object AnnotationProcessor {
            const val roomCompiler = "androidx.room:room-compiler:$version"
        }
    }

    object Coil {
        private const val version = "2.2.2"

        const val coil = "io.coil-kt:coil-compose:$version"
    }

    object Palette {
        private const val version = "1.0.0"

        const val palette = "androidx.palette:palette-ktx:$version"
    }

    object FfmpegKitAudio {
        private const val version = "5.1.LTS"

        const val ffmpegKitAudio = "com.arthenica:ffmpeg-kit-audio:$version"
    }

    object Media3 {
        private const val version = "1.0.0-beta03"

        // For media playback using ExoPlayer
        const val exoplayer = "androidx.media3:media3-exoplayer:$version"

        // For exposing and controlling media sessions
        const val session = "androidx.media3:media3-session:$version"

        @JvmStatic
        val bundle = listOf(
            exoplayer,
            session,
        )
    }

    object WorkManager {
        private const val version = "2.7.1"

        const val work = "androidx.work:work-runtime-ktx:$version"

        object Hilt {
            private const val version = "1.0.0"

            const val work = "androidx.hilt:hilt-work:$version"
            const val kapt = "androidx.hilt:hilt-compiler:$version"
        }
    }

    object DocumentFile {
        private const val version = "1.0.1"

        const val documentFile = "androidx.documentfile:documentfile:$version"
    }

    object Json {
        private const val version = "1.4.0"

        const val kotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:$version"
    }

    object OkHttp {
        private const val version = "4.10.0"

        const val okhttp = "com.squareup.okhttp3:okhttp:$version"
    }

    object Moshi {
        private const val version = "1.14.0"

        const val moshi = "com.squareup.moshi:moshi:$version"
        const val moshiKapt = "com.squareup.moshi:moshi-kotlin-codegen:$version"
    }

    object Logcat {
        private const val version = "0.1"

        const val logcat = "com.squareup.logcat:logcat:$version"
    }

    object JavaX {
        private const val version = "1"

        const val javaXInject = "javax.inject:javax.inject:$version"
    }

    object KtLint {
        private const val version = "0.47.1"

        const val ktlint = "com.pinterest:ktlint:$version"
    }
}

object TestLibraries {

    object AndroidX {
        private const val coreVersion = "1.4.0"

        const val core = "androidx.test:core:$coreVersion"
        const val runner = "androidx.test:runner:$coreVersion"
        const val rules = "androidx.test:rules:$coreVersion"
    }

    object Coroutines {
        const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Libraries.Coroutines.version}"
    }

    object Hilt {
        const val hilt = "com.google.dagger:hilt-android-testing:${Libraries.Hilt.version}"
    }

    object Mavericks {
        const val mavericks = "com.airbnb.android:mavericks-testing:${Libraries.Mavericks.version}"
    }

    object Espresso {
        private const val version = "3.4.0"

        const val espressoCore = "androidx.test.espresso:espresso-core:$version"
    }
}
