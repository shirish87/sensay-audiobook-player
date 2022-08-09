import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    google()
    maven(url = "https://plugins.gradle.org/m2/")
    mavenCentral()
}

plugins {
    `kotlin-dsl`
}

val javaVersion = JavaVersion.VERSION_11

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = javaVersion.toString()
            }
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = javaVersion.toString()
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }
}

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:7.2.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
    implementation("com.squareup:javapoet:1.13.0")
}
