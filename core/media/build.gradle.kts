
plugins {
    id("build-logic.android.library")
}

android {
    namespace = "media"
}

dependencies {
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    // implementation(libs.androidx.media3.datasource.okhttp)
//    implementation(libs.androidx.media3.extractor)
    implementation(libs.media3.extractor.m4b)
    implementation(libs.androidx.media3.exoplayer.workmanager)
    implementation(libs.androidx.documentfile)

    // implementation(libs.kotlinx.coroutines.guava)

    implementation(project(":core:config"))
    implementation(project(":core:data"))
}
