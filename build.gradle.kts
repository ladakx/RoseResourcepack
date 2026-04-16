plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "me.ladakx"
version = "3.5.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    compileOnly("org.spigotmc:spigot-api:26.1-R0.1-SNAPSHOT")
    implementation("com.cjcrafter:foliascheduler:0.7.4-ladakx-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(8)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveVersion.set("")

    archiveFileName.set("RoseResourcepack-$version.jar")
    relocate("com.cjcrafter.foliascheduler", "me.ladakx.roserp.libs.foliascheduler")
    dependencies {
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}
