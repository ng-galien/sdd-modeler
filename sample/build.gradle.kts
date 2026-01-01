plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.sdd.codegen)
}

dependencies {
    implementation(libs.state.modeler.core)
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.spring.ai.starter.mcp.server.webmvc)
    implementation("org.liquibase:liquibase-core")
    
    implementation("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

val modelFileProp = project.findProperty("sddModelFile") as String?
val outDirProp = project.findProperty("sddOutDir") as String?
val languageProp = project.findProperty("sddLanguage") as String?
val addToSourceSetProp = (project.findProperty("sddAddToSourceSet") as String?)?.toBoolean() ?: true
val ddlOutDir = layout.buildDirectory.dir("generated/sdd/ddl")

val resolvedOutputDir = layout.buildDirectory.dir(outDirProp ?: "generated/sdd")

sddCodegen {
    modelFile.set(file(modelFileProp ?: "src/main/resources/sdd.yaml"))
    outputDir.set(layout.buildDirectory.dir(outDirProp ?: "generated/sdd"))
    language.set(languageProp ?: "java")
    addToSourceSet.set(addToSourceSetProp)
    ddlOutputDir.set(ddlOutDir)
    liquibase.set(true)
}

// Ensure generated sources are reformatted with Spotless immediately after generation so
// format checks pass in the lifecycle (e.g., `build`).
tasks.named("generateSddCode") {
    finalizedBy("spotlessApply")
}

// Generate Liquibase changelog and put it on the classpath for Boot/Liquibase.
tasks.named<ProcessResources>("processResources") {
    dependsOn("generateSddDdl", "generateSddCode")
    from(ddlOutDir) {
        include("changelog.yaml")
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("generateSddDdl")
}

// Add generated sources to compilation sourceset if flagged
// With the plugin applied, the `generateSddCode` task is registered and automatically wired into
// the Java source set by the plugin when `addToSourceSet` is true. No further setup required here.

// Exclude generated sources from Spotless formatting checks
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        val generated = resolvedOutputDir.get().asFile
        // Use a path relative to the project dir so Spotless targetExclude matches correctly
        val relativeGenerated = generated.toRelativeString(project.projectDir).replace(File.separatorChar, '/')
        targetExclude("$relativeGenerated/**")
    }
}

// Postgres connection defaults for integration tests in main test source set
tasks.withType<Test>().configureEach {
    // Only add the properties when running tests in this module
    if (project.name == "sample") {
        val pgHost = providers.gradleProperty("pgHost").orElse(providers.environmentVariable("POSTGRES_HOST")).orElse("localhost")
        val pgPort = providers.gradleProperty("pgPort").orElse(providers.environmentVariable("POSTGRES_PORT")).orElse("5432")
        val pgDb = providers.gradleProperty("pgDb").orElse(providers.environmentVariable("POSTGRES_DB")).orElse("sdd_test")
        val pgUser = providers.gradleProperty("pgUser").orElse(providers.environmentVariable("POSTGRES_USER")).orElse("test")
        val pgPass = providers.gradleProperty("pgPass").orElse(providers.environmentVariable("POSTGRES_PASSWORD")).orElse("test")

        systemProperty("POSTGRES_HOST", pgHost.get())
        systemProperty("POSTGRES_PORT", pgPort.get())
        systemProperty("POSTGRES_DB", pgDb.get())
        systemProperty("POSTGRES_USER", pgUser.get())
        systemProperty("POSTGRES_PASSWORD", pgPass.get())
    }
}
