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
    
    implementation("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

val modelFileProp = project.findProperty("sddModelFile") as String?
val outDirProp = project.findProperty("sddOutDir") as String?
val languageProp = project.findProperty("sddLanguage") as String?
val addToSourceSetProp = (project.findProperty("sddAddToSourceSet") as String?)?.toBoolean() ?: true

val resolvedOutputDir = layout.buildDirectory.dir(outDirProp ?: "generated/sdd")

sddCodegen {
    modelFile.set(file(modelFileProp ?: "src/main/resources/sdd.yaml"))
    outputDir.set(layout.buildDirectory.dir(outDirProp ?: "generated/sdd"))
    language.set(languageProp ?: "java")
    addToSourceSet.set(addToSourceSetProp)
}

// Ensure generated sources are reformatted with Spotless immediately after generation so
// format checks pass in the lifecycle (e.g., `build`).
tasks.named("generateSddCode") {
    finalizedBy("spotlessApply")
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
