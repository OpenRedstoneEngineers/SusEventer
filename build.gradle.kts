import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("xyz.jpenilla.run-paper") version "2.0.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

group = ""
version = "1.1"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.aikar.co/nexus/content/groups/aikar/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://libraries.minecraft.net")
    maven("https://repo.essentialsx.net/snapshots/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(group = "co.aikar", name = "acf-paper", version = "0.5.1-SNAPSHOT")
    compileOnly(group = "com.intellectualsites.plotsquared", name = "plotsquared-core", version = "7.3.8")
    compileOnly(group = "com.intellectualsites.plotsquared", name = "plotsquared-bukkit", version = "7.3.8")
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.18.2-R0.1-SNAPSHOT")
    compileOnly(group = "net.essentialsx", name = "EssentialsX", version = "2.20.0-SNAPSHOT")
}


tasks.shadowJar {
    relocate("co.aikar.commands", "SusEventer.acf")
    relocate("co.aikar.locales", "SusEventer.locales")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "16"
        javaParameters = true
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks {
    runServer {
        minecraftVersion("1.18.2")
    }
}
