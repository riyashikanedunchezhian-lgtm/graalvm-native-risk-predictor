plugins {
    id("java-library")
}

dependencies {
    api(libs.bundles.asm)
    api(libs.classgraph)
    implementation(libs.gson)

    testImplementation(libs.junit.jupiter)
}
