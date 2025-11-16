dependencies {
    // JSpecify for null-safety annotations
    compileOnly("org.jspecify:jspecify:${rootProject.ext["jspecifyVersion"]}")
    
    // Jackson for YAML/JSON parsing
    api("com.fasterxml.jackson.core:jackson-core:${rootProject.ext["jacksonVersion"]}")
    api("com.fasterxml.jackson.core:jackson-databind:${rootProject.ext["jacksonVersion"]}")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${rootProject.ext["jacksonVersion"]}")
    
    // Additional Jackson modules for Java time, etc.
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${rootProject.ext["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:${rootProject.ext["jacksonVersion"]}")
    
    // Vavr for functional programming and enhanced error handling
    implementation("io.vavr:vavr:0.10.7")
    
    // JSON Schema generation for IDE support (victools)
    implementation("com.github.victools:jsonschema-generator:4.38.0")
    implementation("com.github.victools:jsonschema-module-jackson:4.28.0")
    implementation("com.github.victools:jsonschema-module-jakarta-validation:4.38.0")

    // Test dependencies for core functionality
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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