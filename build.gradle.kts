val ktorVersion: String by project
val kotlinLoggingVersion: String by project
val logbackVersion: String by project
val spekVersion: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

//This is necessary to make the version accessible in other places
val kotlinVersion: String? by extra {
    buildscript.configurations["classpath"]
        .resolvedConfiguration.firstLevelModuleDependencies
        .find { it.moduleName == "kotlin-gradle-plugin" }?.moduleVersion
}

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
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}