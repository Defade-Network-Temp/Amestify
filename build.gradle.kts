plugins {
    id("java")
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
    implementation("com.formdev:flatlaf:3.0")
    implementation("org.swinglabs:swingx:1.6.1")
    implementation("org.mongodb:mongo-java-driver:3.12.11")
    implementation("net.defade:Yokura:1.19.2-R1.2-SNAPSHOT")
}