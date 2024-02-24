
plugins {
    id("build-logic.android.library")
}

android {
    namespace = "remote"
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.moshi)
}
