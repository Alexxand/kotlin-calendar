val ktorVersion: String by project
val exposedVersion: String by project
val flywayVersion:String by project
val hikariVersion: String by project
val postgresqlDriverVersion: String by project
val kotlinLoggingVersion: String by project
val logbackVersion: String by project
val kotlinxSerializationVersion: String by project
val spekVersion: String by project

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    application
    id("com.palantir.docker") version "0.32.0"
    id("com.avast.gradle.docker-compose") version "0.15.0"
}

fun getPluginVersion(pluginName: String): String =
    buildscript.configurations["classpath"]
        .resolvedConfiguration.firstLevelModuleDependencies
        .find { it.moduleName == pluginName }!!.moduleVersion

//This is necessary as you cannot use external variables inside the plugins {} block
val kotlinVersion: String = getPluginVersion("org.jetbrains.kotlin.jvm.gradle.plugin")

group = "com.github.Alexxand"
version = "1.0"

repositories {
    mavenCentral()
}

application {
    mainClass.set("ApplicationKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.postgresql:postgresql:$postgresqlDriverVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}

docker {
    name = project.name
    files(tasks.distTar.get().outputs)
}