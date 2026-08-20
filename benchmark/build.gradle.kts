plugins {
    id("application")
}

dependencies {
    implementation(project(":native-risk-core"))
    implementation(libs.gson)
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("io.nativerisk.benchmark.PrecisionRecallReport")
}
