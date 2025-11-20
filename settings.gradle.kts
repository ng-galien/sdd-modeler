pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    // Register the plugin build so the plugin can be resolved without publishing
    // NOTE: switched to a regular subproject to enable local project substitution and simpler dependency resolution
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "sdd-modeler"

include("state-modeler-core")
include("state-modeler-app")
include("sample")

// Include the plugin as a regular subproject so it can reference the core project directly
include("state-modeler-gradle-plugin")
