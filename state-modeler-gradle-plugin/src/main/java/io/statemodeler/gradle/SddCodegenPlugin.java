package io.statemodeler.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * Gradle plugin wiring code generation tasks into Java projects.
 */
public class SddCodegenPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        var extension = project.getExtensions().create("sddCodegen", SddCodegenExtension.class, project);

        TaskProvider<GenerateSddCodeTask> generateTask = project.getTasks()
                .register("generateSddCode", GenerateSddCodeTask.class, task -> {
                    task.setGroup("sdd");
                    task.setDescription("Generate sources from an SDD model file");
                    task.getModelFile().convention(extension.getModelFile());
                    task.getOutputDir().convention(extension.getOutputDir());
                    task.getLanguage().convention(extension.getLanguage());
                });

        project.getPluginManager()
                .withPlugin("java", applied -> configureJavaIntegration(project, extension, generateTask));
    }

    private void configureJavaIntegration(
            Project project, SddCodegenExtension extension, TaskProvider<GenerateSddCodeTask> generateTask) {
        boolean addToSourceSet = extension.getAddToSourceSet().getOrElse(true);
        if (!addToSourceSet) return;

        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME, main -> {
            main.getJava().srcDir(generateTask.flatMap(task -> task.getOutputDir()));
        });

        project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME).configure(task -> task.dependsOn(generateTask));
    }
}
