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
    
    // H2 Database (embedded) for SDR repository
    implementation("com.h2database:h2:2.2.224")
    
    // JSON diff library for schema comparison
    implementation("com.flipkart.zjsonpatch:zjsonpatch:0.4.16")
    
    // Diff algorithm for DDL comparison
    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    
    // LangChain4j for LLM-based migration script generation
    implementation("dev.langchain4j:langchain4j:0.36.2")
    implementation("dev.langchain4j:langchain4j-jlama:0.36.2")
    implementation("dev.langchain4j:langchain4j-ollama:0.36.2")
    
    // SLF4J for logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.12")

    // Test dependencies specific to CLI
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}