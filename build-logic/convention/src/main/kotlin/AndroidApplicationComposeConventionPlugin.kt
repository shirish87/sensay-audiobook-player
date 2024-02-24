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

import build.helpers.configureAndroidCompose
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {

    companion object {
        fun configure(target: Project) {
            with(target) {
                AndroidApplicationConventionPlugin.configure(target)

                val extension = extensions.getByType<ApplicationExtension>()
                configureAndroidCompose(extension)

                val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

                dependencies {
                    add("implementation", libs.findLibrary("android.material").get())
                    add("implementation", libs.findLibrary("androidx.activity.compose").get())
                    add("implementation", libs.findLibrary("androidx.compose.ui").get())
                    add("implementation", libs.findLibrary("androidx.compose.ui.graphics").get())
                    add("implementation", libs.findLibrary("androidx.compose.ui.tooling.preview").get())
                    add("implementation", libs.findLibrary("androidx.compose.material3").get())
                    add("implementation", libs.findLibrary("androidx.compose.material3.windowSizeClass").get())
                    add("implementation", libs.findLibrary("androidx.compose.foundation").get())
                    add("implementation", libs.findLibrary("androidx.compose.foundation.layout").get())
                    add("implementation", libs.findLibrary("androidx.lifecycle.runtime.ktx").get())

                    add("androidTestImplementation", libs.findLibrary("androidx.compose.ui.test.junit4").get())
                    add("debugImplementation", libs.findLibrary("androidx.compose.ui.test.manifest").get())
                    add("debugImplementation", libs.findLibrary("androidx.compose.ui.tooling").get())
                }
            }
        }
    }

    override fun apply(target: Project) = configure(target)
}
