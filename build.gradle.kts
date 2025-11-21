plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.axion.release)
    jacoco
}

scmVersion {
    tag {
        prefix.set("v")
    }
    versionCreator("versionWithBranch")
    checks {
        uncommittedChanges.set(false)
    }
}

allprojects {
    group = "io.statemodeler"
    version = rootProject.scmVersion.version

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

// Extract versions from catalog to avoid scope issues in subprojects
val palantirJavaFormatVersion = libs.versions.palantir.java.format.get()
val jacocoVersion = libs.versions.jacoco.get()

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "jacoco")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
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
            palantirJavaFormat(palantirJavaFormatVersion)
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
            formatAnnotations()
        }
        
        flexmark {
            target("*.md", "**/*.md")
            // Flexmark formatting with defaults (which handle trailing spaces properly)
            flexmark()
            endWithNewline()
        }
    }

    // JaCoCo configuration for code coverage
    jacoco {
        toolVersion = jacocoVersion
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.withType<JacocoReport>())
        systemProperty("java.awt.headless", "true")
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

    // Ensure an SLF4J provider is available for all subprojects to avoid per-module duplications
    afterEvaluate {
        dependencies {
            // Use the version catalog `libs` to add logback so the version is synchronized across the project
            add("runtimeOnly", libs.logback.classic)
            add("testRuntimeOnly", libs.logback.classic)
        }
    }
}

// Version catalog for shared dependencies
// Versions are now managed in gradle/libs.versions.toml

// Task to copy schema from core resources to project root for GitHub distribution
val coreProject = findProject(":state-modeler-core")
// If core is an included build instead of a subproject then the task path
// `:state-modeler-core:generateJsonSchema` isn't available; configure dependsOn
// only when the core project is present in this build.
tasks.register<Copy>("distributeSchema") {
    group = "distribution"
    description = "Copies the generated JSON Schema from core module to project root for GitHub distribution"
    
    if (coreProject != null) {
        dependsOn(":state-modeler-core:generateJsonSchema")
    }
    from("state-modeler-core/src/main/resources/sdd-model-schema.json")
    into(projectDir)
    
    doLast {
        println("📋 Schema distributed to project root for GitHub")
        println("🌍 Available at: https://github.com/user/repo/blob/main/sdd-model-schema.json")
    }
}

// Aggregated JaCoCo report for all modules
tasks.register<JacocoReport>("jacocoAggregatedReport") {
    group = "verification"
    description = "Generates an aggregated JaCoCo coverage report for all modules"
    
    dependsOn(subprojects.map { it.tasks.withType<Test>() })
    // Only enforce ordering if the core project exists in this build. When the core
    // is part of a composite (included) build, the generateJsonSchema task is not
    // visible via the project path `:state-modeler-core:generateJsonSchema` and would
    // cause a configuration-time error.
    if (coreProject != null) {
        mustRunAfter(":state-modeler-core:generateJsonSchema")
    }
    
    sourceDirectories.setFrom(subprojects.flatMap { project ->
        project.the<SourceSetContainer>()["main"].allJava.srcDirs
    })
    
    classDirectories.setFrom(subprojects.flatMap { project ->
        project.the<SourceSetContainer>()["main"].output.classesDirs
    })
    
    executionData.setFrom(subprojects.mapNotNull { project ->
        val jacocoFile = project.layout.buildDirectory.file("jacoco/test.exec").get().asFile
        if (jacocoFile.exists()) jacocoFile else null
    })
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    finalizedBy("jacocoAggregatedCoverageVerification")
}

// Coverage verification
tasks.register<JacocoCoverageVerification>("jacocoAggregatedCoverageVerification") {
    group = "verification"
    description = "Verifies minimum code coverage thresholds"
    
    dependsOn("jacocoAggregatedReport")
    
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
        
        rule {
            enabled = false
            element = "CLASS"
            includes = listOf("io.statemodeler.core.*")
            
            limit {
                counter = "LINE"
                value = "TOTALCOUNT"
                maximum = "200".toBigDecimal()
            }
        }
    }
}