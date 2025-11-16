plugins {
    application
}

application {
    mainClass = "io.statemodeler.cli.Main"
}

dependencies {
    // Depend on core module
    implementation(project(":state-modeler-core"))
    
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    
    // SLF4J for logging
    implementation("org.slf4j:slf4j-api:${rootProject.ext["slf4jVersion"]}")
    runtimeOnly("org.slf4j:slf4j-simple:${rootProject.ext["slf4jVersion"]}")
    
    // Picocli for CLI
    implementation("info.picocli:picocli:${rootProject.ext["picocliVersion"]}")
    annotationProcessor("info.picocli:picocli-codegen:${rootProject.ext["picocliVersion"]}")

    // Test dependencies specific to CLI
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}