group = "io.statemodeler"
version = "0.1.0-SNAPSHOT"

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(gradleApi())
    //Depends on core module
    implementation("io.statemodeler:state-modeler-core:0.1.0-SNAPSHOT")
}

gradlePlugin {
    plugins {
        create("sddCodegen") {
            id = "io.statemodeler.sdd-codegen"
            displayName = "SDD Modeler code generation plugin"
            description = "Generates source code from SDD model files using the SDD Modeler core library."
            implementationClass = "io.statemodeler.gradle.SddCodegenPlugin"
        }
    }
}