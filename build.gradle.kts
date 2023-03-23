plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "net.defade"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.defade.net/defade") {
        name = "defade"
        credentials(PasswordCredentials::class)
    }
}

dependencies {
    implementation("com.github.luben:zstd-jni:1.5.2-5")
    implementation("org.mongodb:mongo-java-driver:3.12.11")
    implementation("com.github.Querz:NBT:6.1")
    implementation("it.unimi.dsi:fastutil:8.5.9")
    implementation("com.github.Minestom:MinestomDataGenerator:1c1921cd41")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains:annotations:23.0.0")
}