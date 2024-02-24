/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "build-logic.buildlogic"

val javaVersion = JavaVersion.valueOf("VERSION_${libs.versions.jvmTarget.get()}")
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.kotlin.ksp.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplicationComposeMavericks") {
            id = "build-logic.android.application.compose.mavericks"
            implementationClass = "AndroidApplicationComposeMavericksConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "build-logic.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "build-logic.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "build-logic.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidLibraryRoom") {
            id = "build-logic.android.library.room"
            implementationClass = "AndroidLibraryRoomConventionPlugin"
        }
        register("androidLibrarySerialization") {
            id = "build-logic.android.library.serialization"
            implementationClass = "AndroidLibrarySerializationConventionPlugin"
        }
        register("androidLibrary") {
            id = "build-logic.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidTest") {
            id = "build-logic.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
    }
}
