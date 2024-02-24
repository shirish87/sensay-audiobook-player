/*
 * Copyright 2022 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import build.helpers.addCommonDependencies
import build.helpers.configureGradleManagedDevices
import build.helpers.configureKotlinAndroid
import build.helpers.configurePrintApksTask
import build.helpers.disableUnnecessaryAndroidTests
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryConventionPlugin : Plugin<Project> {

    companion object {
        fun configure(target: Project) {
            with(target) {
                with(pluginManager) {
                    apply("com.android.library")
                    apply("org.jetbrains.kotlin.android")
                    apply("com.google.devtools.ksp")
                    apply("com.google.dagger.hilt.android")
                    apply("org.jetbrains.kotlin.plugin.parcelize")

                    // KAPT must go last to avoid build warnings.
                    // See: https://stackoverflow.com/questions/70550883/warning-the-following-options-were-not-recognized-by-any-processor-dagger-f
                    apply("org.jetbrains.kotlin.kapt")
                }

                val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

                extensions.configure<LibraryExtension> {
                    configureKotlinAndroid(this)
                    defaultConfig.targetSdk = libs.findVersion("sdkTarget").get().toString().toInt()
                    configureGradleManagedDevices(this)
                }
                extensions.configure<LibraryAndroidComponentsExtension> {
                    configurePrintApksTask(this)
                    disableUnnecessaryAndroidTests(target)
                }

                addCommonDependencies()

                dependencies {
                    add("kapt", libs.findLibrary("hilt.compiler").get())
                    add("api", libs.findLibrary("hilt").get())
                    add("api", libs.findLibrary("javax.inject").get())
                }
            }
        }
    }

    override fun apply(target: Project) = configure(target)
}
