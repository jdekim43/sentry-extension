import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date

plugins {
    kotlin("jvm") version "1.3.70"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}

val artifactName = "sentry-extension"
val artifactGroup = "kr.jadekim"
val artifactVersion = "1.0.0"
group = artifactGroup
version = artifactVersion

repositories {
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/jdekim43/maven")
}

dependencies {
    val jLoggerVersion: String by project
    val ktorExtensionVersion: String by project
    val kotlinxCoroutineVersion: String by project
    val sentryVersion: String by project
    val jacksonVersion: String by project

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutineVersion")
    implementation("kr.jadekim:j-logger:$jLoggerVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    api("io.sentry:sentry:$sentryVersion") {
        exclude("org.slf4j", "slf4j-api")
        exclude("com.fasterxml.jackson.core", "jackson-core")
    }

    compileOnly("kr.jadekim:ktor-extension:$ktorExtensionVersion")
}

tasks.withType<KotlinCompile> {
    val jvmTarget: String by project

    kotlinOptions.jvmTarget = jvmTarget
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("lib") {
            groupId = artifactGroup
            artifactId = artifactName
            version = artifactVersion
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    publish = true

    setPublications("lib")

    pkg.apply {
        repo = "maven"
        name = rootProject.name
        setLicenses("MIT")
        setLabels("kotlin", "sentry")
        vcsUrl = "https://github.com/jdekim43/sentry-extension.git"
        version.apply {
            name = artifactVersion
            released = Date().toString()
        }
    }
}