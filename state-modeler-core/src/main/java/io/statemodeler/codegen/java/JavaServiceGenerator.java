package io.statemodeler.codegen.java;

import io.pebbletemplates.pebble.PebbleEngine;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import java.util.HashMap;
import java.util.Map;

public class JavaServiceGenerator extends JavaGeneratorBase {

    public JavaServiceGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        super(engine, contextBuilder);
    }

    @Override
    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        java.util.List<String> autoConfigurationClasses = new java.util.ArrayList<>();

        java.util.List<EntityDef> sortedEntities = new java.util.ArrayList<>(model.entities().values());
        sortedEntities.sort(java.util.Comparator.comparing(EntityDef::name));
        for (EntityDef entity : sortedEntities) {
            // Interface
            String interfaceContent = generateFile(entity, model, "templates/java/service_interface.java.pebble");
            String interfaceName = contextBuilder.toPascal(entity.name()) + "Service";
            generatedFiles.put(resolveFilename(model, interfaceName), interfaceContent);

            // Default Implementation
            String implContent = generateFile(entity, model, "templates/java/service_default_impl.java.pebble");
            String implName = "Default" + contextBuilder.toPascal(entity.name()) + "Service";
            generatedFiles.put(resolveFilename(model, implName), implContent);

            // AutoConfiguration
            String configContent = generateFile(entity, model, "templates/java/service_autoconfiguration.java.pebble");
            String configName = contextBuilder.toPascal(entity.name()) + "ServiceAutoConfiguration";
            generatedFiles.put(resolveFilename(model, configName), configContent);

            // Collect AutoConfiguration class name
            var options = model.database().generatorOptions();
            var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
            autoConfigurationClasses.add(pkg + "." + configName);
        }

        // Generate AutoConfiguration.imports
        if (!autoConfigurationClasses.isEmpty()) {
            String importsContent = String.join("\n", autoConfigurationClasses);
            generatedFiles.put(
                    "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports", importsContent);
        }

        return generatedFiles;
    }
}
