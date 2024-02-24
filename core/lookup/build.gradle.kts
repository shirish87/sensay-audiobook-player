
plugins {
    id("build-logic.android.library")
}

android {
    namespace = "lookup"
}

dependencies {
    implementation(project(":core:remote"))
    api(libs.okhttp)
    api(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
}
