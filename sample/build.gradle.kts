plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    id("io.statemodeler.sdd-codegen")
}

dependencies {
    implementation("io.statemodeler:state-modeler-core")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}

val modelFileProp = project.findProperty("sddModelFile") as String?
val outDirProp = project.findProperty("sddOutDir") as String?
val languageProp = project.findProperty("sddLanguage") as String?
val addToSourceSetProp = (project.findProperty("sddAddToSourceSet") as String?)?.toBoolean() ?: false

val resolvedOutputDir = layout.buildDirectory.dir(outDirProp ?: "generated/sdd")

val modelFileUsedAbs = project.file(modelFileProp ?: "src/main/resources/sdd.yaml").absolutePath
val resolvedOutDirFile = if (outDirProp != null) project.file(outDirProp) else project.buildDir.resolve("generated/sdd")
val sddOutDirAbs = resolvedOutDirFile.absolutePath

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
