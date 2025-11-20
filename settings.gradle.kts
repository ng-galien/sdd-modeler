pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    // NOTE: plugin project is a regular subproject - we will publish to mavenLocal and
    // resolve the plugin id from local repository instead of an included build.
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

// Keep the plugin project as a subproject for project dependencies, but also register the build
// so that the plugin id is resolvable by subprojects that use the plugins DSL.
include("state-modeler-gradle-plugin")
