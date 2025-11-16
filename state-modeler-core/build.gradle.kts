dependencies {
    // Jackson for YAML/JSON parsing
    api("com.fasterxml.jackson.core:jackson-core:${rootProject.ext["jacksonVersion"]}")
    api("com.fasterxml.jackson.core:jackson-databind:${rootProject.ext["jacksonVersion"]}")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${rootProject.ext["jacksonVersion"]}")
    
    // Additional Jackson modules for Java time, etc.
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${rootProject.ext["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:${rootProject.ext["jacksonVersion"]}")

    // Test dependencies for core functionality
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}