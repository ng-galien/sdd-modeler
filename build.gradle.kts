plugins {
    id("com.diffplug.spotless") version "6.25.0"
}

allprojects {
    group = "io.statemodeler"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            palantirJavaFormat("2.82.0")
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
            formatAnnotations()
        }
    }
}

// Version catalog for shared dependencies
val jacksonVersion = "2.18.0"
val picocliVersion = "4.7.6"

ext["jacksonVersion"] = jacksonVersion
ext["picocliVersion"] = picocliVersion