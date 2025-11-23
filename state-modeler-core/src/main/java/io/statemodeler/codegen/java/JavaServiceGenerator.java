package io.statemodeler.codegen.java;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JavaServiceGenerator {

    private final PebbleEngine engine;
    private final JavaContextBuilder contextBuilder;

    public JavaServiceGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        this.engine = engine;
        this.contextBuilder = contextBuilder;
    }

    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        java.util.List<String> autoConfigurationClasses = new java.util.ArrayList<>();

        for (EntityDef entity : model.entities().values()) {
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

    private String generateFile(EntityDef entity, SddModel model, String templatePath) {
        PebbleTemplate template = engine.getTemplate(templatePath);
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = contextBuilder.buildEntityContext(entity);
        context.put("entity", entityCtx);
        Map<String, Object> modelCtx = contextBuilder.buildModelContext(model);
        context.put("model", modelCtx);

        Set<String> imports = new HashSet<>();
        Object modelImps = modelCtx.get("imports");
        if (modelImps instanceof Set<?> mis) {
            for (Object o : mis) if (o instanceof String str) imports.add(str);
        }
        Object entityImps = entityCtx.get("imports");
        if (entityImps instanceof Set<?> eis) {
            for (Object o : eis) if (o instanceof String str) imports.add(str);
        }
        context.put("imports", imports);
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate file for " + entity.name() + " using " + templatePath, e);
        }
    }

    private String resolveFilename(SddModel model, String className) {
        var options = model.database().generatorOptions();
        var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
        return pkg.replace('.', '/') + "/" + className + ".java";
    }
}
