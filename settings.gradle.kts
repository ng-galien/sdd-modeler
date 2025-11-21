pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    // Include the plugin as a composite build so the plugin id can be used in the plugins DSL
    includeBuild("state-modeler-gradle-plugin")
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "sdd-modeler"

include("state-modeler-app")
include("sample")
include("state-modeler-app")
include("sample")
includeBuild("state-modeler-core")
// The Gradle plugin project is included as a composite build via pluginManagement.includeBuild so
// it resolves as a plugin dependency (plugin DSL). We don't include it as a subproject here to
// avoid duplicate project names across the main build and included Builds.

includeBuild("state-modeler-gradle-plugin")

