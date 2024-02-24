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

import build.helpers.addCommonAppDependencies
import build.helpers.addCommonDependencies
import build.helpers.configureGradleManagedDevices
import build.helpers.configureKotlinAndroid
import build.helpers.configurePrintApksTask
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType


class AndroidApplicationConventionPlugin : Plugin<Project> {

    companion object {

        fun configure(target: Project) {
            with(target) {
                with(pluginManager) {
                    apply("com.android.application")
                    apply("org.jetbrains.kotlin.android")
                    apply("com.google.dagger.hilt.android")
                    apply("org.jetbrains.kotlin.plugin.parcelize")

                    // KAPT must go last to avoid build warnings.
                    // See: https://stackoverflow.com/questions/70550883/warning-the-following-options-were-not-recognized-by-any-processor-dagger-f
                    apply("org.jetbrains.kotlin.kapt")
                }

                val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

                extensions.configure<ApplicationExtension> {
                    configureKotlinAndroid(this)

                    defaultConfig.apply {
                        testNamespace = "${namespace}.test"

                        targetSdk = libs.findVersion("sdkTarget").get().toString().toInt()
                        versionCode = libs.findVersion("appVersionCode").get().toString().toInt()
                        versionName = libs.findVersion("appVersionName").get().toString()
                    }

                    buildTypes {
                        getByName("debug") {
                            applicationIdSuffix = ".debug"
                        }

                        getByName("release") {
                            isMinifyEnabled = true
                            isShrinkResources = true

                            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                        }
                    }

                    buildFeatures {
                        buildConfig = true
                    }

                    configureGradleManagedDevices(this)
                }
                extensions.configure<ApplicationAndroidComponentsExtension> {
                    configurePrintApksTask(this)
                }

                addCommonDependencies()
                addCommonAppDependencies()
            }
        }
    }

    override fun apply(target: Project) = configure(target)
}
