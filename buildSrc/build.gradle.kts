repositories {
    google()
    maven(url = "https://plugins.gradle.org/m2/")
    mavenCentral()
}

plugins {
    `kotlin-dsl`
}

val javaVersion = JavaVersion.VERSION_11

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(
            JavaLanguageVersion.of(javaVersion.toString())
        )
    }

    kotlinDslPluginOptions {
        jvmTarget.set(javaVersion.toString())
    }
}

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:7.4.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    implementation("com.squareup:javapoet:1.13.0")
}
