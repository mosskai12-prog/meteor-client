plugins {
    id("fabric-loom") version "1.14-SNAPSHOT"
}

group = "dev.fizzyclient"
version = "1.0.0"

base {
    archivesName.set("FizzyClient")
}

repositories {
    maven("https://maven.fabricmc.net/")
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.11")
    mappings("net.fabricmc:yarn:1.21.11+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.18.2")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.140.0+1.21.11")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
