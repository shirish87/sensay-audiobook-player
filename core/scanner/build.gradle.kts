
plugins {
    id("build-logic.android.library.serialization")
}

android {
    namespace = "scanner"
}

dependencies {
    implementation(libs.androidx.documentfile)
}
