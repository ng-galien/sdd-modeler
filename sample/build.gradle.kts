plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
}

dependencies {
    implementation(project(":state-modeler-core"))
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("com.h2database:h2")
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

val generateCode by tasks.registering(Exec::class) {
    group = "sdd"
    description = "Runs the scripts/generate-code.sh helper to produce generated sources for the sample module"

    val scriptPath = project.rootProject.file("scripts/generate-code.sh").absolutePath
    commandLine("bash", scriptPath,
        "-m", modelFileUsedAbs,
        "-o", sddOutDirAbs,
        "-l", languageProp ?: "java",
        "--skip-build",
        "--no-format")

    outputs.dir(sddOutDirAbs)

    doFirst {
        println("Generating sources from model: $modelFileUsedAbs into $sddOutDirAbs")
        project.file(sddOutDirAbs).mkdirs()
    }
}

// Add generated sources to compilation sourceset if flagged
if (addToSourceSetProp == null || addToSourceSetProp) {
    sourceSets.main.get().java.srcDir(resolvedOutputDir)
    tasks.named("compileJava") {
        dependsOn(generateCode)
    }
}

// Exclude generated sources from Spotless formatting checks
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        val generated = resolvedOutputDir.get().asFile
        targetExclude("${generated.absolutePath}/**")
    }
}
