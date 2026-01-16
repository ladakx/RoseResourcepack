import java.util.*

plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "me.emsockz"
version = "3.3.5"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

repositories {
    mavenCentral()
    maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot API
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") // Adventure Snapshots
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.4")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")

    compileOnly("org.spigotmc:spigot-api:1.21.3-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(16)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveVersion.set("")

    archiveFileName.set("RoseResourcepack-$version.jar")
    dependencies {
        relocate("net.kyori", "me.emsockz.roserp.libs.kyori")
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

val properties = Properties().apply {
    load(file("deployment.properties").inputStream())
}

val deployPlugin by tasks.registering {
    group = "deployment"
    description = "Deploys the plugin to the Minecraft server after building."

    val pluginFile = file("build/libs/RoseResourcepack-${version}.jar")
    val serverIp = properties["serverIp"] as String
    val remotePath = properties["remotePath"] as String
    val username = properties["username"] as String
    val privateKeyPath = properties["privateKeyPath"] as String

    doLast {
        if (pluginFile.exists()) {
            exec {
                commandLine("scp", "-i", privateKeyPath, pluginFile.absolutePath, "$username@$serverIp:$remotePath")
            }
            exec {
                commandLine("ssh", "-i", privateKeyPath, "$username@$serverIp", "screen -S dev -X stuff '\n'")
            }
            exec {
                commandLine("ssh", "-i", privateKeyPath, "$username@$serverIp", "screen -S dev -X stuff 'say RoseResourcepack deploy success\n'")
            }

            println("Plugin loaded")
        } else {
            println("File not found: ${pluginFile.absolutePath}")
        }
    }
}

tasks.named("build") {
    finalizedBy(deployPlugin)
}