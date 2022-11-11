

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project

val ktlintVersion = "0.41.0"

plugins {
    application
    kotlin("jvm") version "1.7.20"
    id("io.ktor.plugin") version "2.1.2"
    val ktlintPluginVersion = "10.0.0"
    id("org.jlleitschuh.gradle.ktlint") version ktlintPluginVersion
    id("org.jlleitschuh.gradle.ktlint-idea") version ktlintPluginVersion
}

group = "com.example"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    ktlint("com.pinterest:ktlint:$ktlintVersion")


    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.2.21")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
}

ktlint {
    verbose.set(true)
    disabledRules.add("import-ordering")
}
