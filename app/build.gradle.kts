
plugins {
    id("build-logic.android.application.compose.mavericks")
}

android {
    val pkg = "com.dotslashlabs.sensay"
    namespace = pkg
    testNamespace = "$pkg.test"

    defaultConfig {
        applicationId = pkg
    }
}

dependencies {
    implementation(project(":core:config"))
    implementation(project(":core:media"))
    implementation(project(":core:data"))
    implementation(project(":core:lookup"))
    implementation(project(":core:icons"))

    api(libs.androidx.media3.session)
    api(libs.media3.extractor.m4b)
}
