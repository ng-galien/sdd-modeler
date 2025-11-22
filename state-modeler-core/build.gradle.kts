plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.spotless)
    jacoco
}

group = "io.statemodeler"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val palantirJavaFormatVersion = libs.versions.palantir.java.format.get()
val jacocoVersion = libs.versions.jacoco.get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.withType<JacocoReport>())
    systemProperty("java.awt.headless", "true")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        palantirJavaFormat(palantirJavaFormatVersion)
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        formatAnnotations()
    }
    
    flexmark {
        target("*.md", "**/*.md")
        flexmark()
        endWithNewline()
    }
}

jacoco {
    toolVersion = jacocoVersion
}

tasks.withType<JacocoReport> {
    dependsOn(tasks.withType<Test>())
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec"))
}

dependencies {
    // Lombok for reducing boilerplate
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    
    // JSpecify for null-safety annotations
    compileOnly(libs.jspecify)
    
    // Jackson for YAML/JSON parsing
    api(libs.jackson.core)
    api(libs.jackson.databind)
    api(libs.jackson.dataformat.yaml)
    
    // Additional Jackson modules for Java time, etc.
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.module.parameter.names)
    
    // Vavr for functional programming and enhanced error handling
    api(libs.vavr)
    
    // JSON Schema generation for IDE support (victools)
    implementation(libs.victools.jsonschema.generator)
    implementation(libs.victools.jsonschema.module.jackson)
    implementation(libs.victools.jsonschema.module.jakarta.validation)
    
    // Pebble template engine for code generation
    implementation(libs.pebble)
    // Apache Commons Text for robust case conversions in code generation
    implementation(libs.commons.text)
    // Palantir Java Format for formatting generated code
    implementation(libs.palantir.java.format)

    // Test dependencies for core functionality
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj)
    // Test/runtime logback deps are provided globally by the root `subprojects` configuration
    
    // PostgreSQL integration tests with Testcontainers
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.postgresql)
}

// Task to generate JSON Schema automatically during build
val generateJsonSchema by tasks.registering(JavaExec::class) {
    group = "schema"
    description = "Generates JSON Schema for SDD models and saves to src/main/resources"
    
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.statemodeler.schema.SchemaGeneratorTask")
    workingDir = projectDir
    
    inputs.files(sourceSets.main.get().allSource.filter { it.name.endsWith(".java") })
    outputs.file("src/main/resources/sdd-model-schema.json")
}

// Generate schema as part of the build process, but after main compilation
tasks.build {
    dependsOn(generateJsonSchema)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
