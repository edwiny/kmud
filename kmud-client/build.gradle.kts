import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "edwin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}



tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Wrapper> {
    gradleVersion = "7.3"
}

application {
    mainClass.set("MainKt")
}

val ktor_version: String by project

dependencies {
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")

}

// Required by the 'shadowJar' task
project.setProperty("mainClassName", "MainKt")

tasks {
    named<JavaExec>("run") {
        standardInput = System.`in`
    }


    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
            archiveVersion.set("")
            archiveClassifier.set("")
            mergeServiceFiles()
            manifest {
                attributes(mapOf("Main-Class" to "MainKt"))
            }
        }
}

