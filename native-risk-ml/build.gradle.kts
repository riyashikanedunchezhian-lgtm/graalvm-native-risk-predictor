plugins {
    id("java-library")
}

dependencies {
    api(project(":native-risk-core"))
    implementation(libs.smile.core)

    testImplementation(libs.junit.jupiter)
}
