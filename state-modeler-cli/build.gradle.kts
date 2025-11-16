plugins {
    application
}

application {
    mainClass = "io.statemodeler.cli.Main"
}

dependencies {
    // Lombok for reducing boilerplate
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    
    // Depend on core module
    implementation(project(":state-modeler-core"))
    
    // Vavr for functional programming (needed for Try<T> from core)
    implementation("io.vavr:vavr:0.10.7")
    
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