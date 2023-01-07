plugins {
    id("java")
}

group = "net.defade"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:3.0")
    implementation("org.swinglabs:swingx:1.6.1")
}