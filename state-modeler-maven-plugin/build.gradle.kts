import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.spotless)
}

group = "io.statemodeler"
version = libs.versions.sdd.version.get()
base.archivesName.set("sdd-maven-plugin")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":state-modeler-core"))

    compileOnly(libs.maven.plugin.api)
    compileOnly(libs.maven.core)
    compileOnly(libs.maven.plugin.annotations)
    annotationProcessor(libs.maven.plugin.annotations)

    compileOnly(libs.slf4j.api)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.maven.plugin.api)
    testImplementation(libs.maven.core)
    testImplementation(libs.maven.plugin.annotations)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}

tasks.processResources {
    filesMatching("META-INF/maven/plugin.xml") {
        filter<ReplaceTokens>("tokens" to mapOf("projectVersion" to project.version.toString()))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "sdd-maven-plugin"
            pom {
                name.set("SDD Modeler Maven Plugin")
                packaging = "maven-plugin"
            }
        }
    }
}
