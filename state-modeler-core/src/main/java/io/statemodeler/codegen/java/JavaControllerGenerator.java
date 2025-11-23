package io.statemodeler.codegen.java;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import io.statemodeler.core.StateDef;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JavaControllerGenerator {

    private final PebbleEngine engine;
    private final JavaContextBuilder contextBuilder;

    public JavaControllerGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        this.engine = engine;
        this.contextBuilder = contextBuilder;
    }

    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        for (EntityDef entity : model.entities().values()) {
            // Generate interface
            String interfaceContent = generateControllerInterface(entity, model);
            String interfaceFilename = resolveControllerInterfaceFilename(entity, model);
            generatedFiles.put(interfaceFilename, interfaceContent);

            // Generate implementation
            String implContent = generateControllerImplementation(entity, model);
            String implFilename = resolveControllerFilename(entity, model);
            generatedFiles.put(implFilename, implContent);
        }
        return generatedFiles;
    }

    private String generateControllerInterface(EntityDef entity, SddModel model) {
        PebbleTemplate template = engine.getTemplate("templates/java/controller_interface.java.pebble");
        Map<String, Object> context = buildContext(entity, model);

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Controller Interface for " + entity.name(), e);
        }
    }

    private String generateControllerImplementation(EntityDef entity, SddModel model) {
        PebbleTemplate template = engine.getTemplate("templates/java/controller.java.pebble");
        Map<String, Object> context = buildContext(entity, model);

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Controller Implementation for " + entity.name(), e);
        }
    }

    private Map<String, Object> buildContext(EntityDef entity, SddModel model) {
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = contextBuilder.buildEntityContext(entity);
        context.put("entity", entityCtx);
        Map<String, Object> modelCtx = contextBuilder.buildModelContext(model);
        context.put("model", modelCtx);

        // Add transitions context for state-based entities
        if (!entity.states().isEmpty()) {
            List<Map<String, Object>> transitions = contextBuilder.buildTransitionsContext(entity);
            context.put("transitions", transitions);

            // Add state repositories as list
            List<Map<String, String>> stateRepositories = new ArrayList<>();
            for (StateDef state : entity.states().values()) {
                Map<String, String> stateRepo = new HashMap<>();
                stateRepo.put("stateName", state.name());
                stateRepo.put("className", contextBuilder.toPascal(state.name()));
                stateRepo.put("repositoryName", contextBuilder.toPascal(state.name()) + "Repository");
                stateRepo.put("propertyName", contextBuilder.toCamel(state.name()));
                stateRepositories.add(stateRepo);
            }
            context.put("stateRepositories", stateRepositories);
        }

        Set<String> imports = new HashSet<>();
        Object modelImps = modelCtx.get("imports");
        if (modelImps instanceof Set<?> mis) {
            for (Object o : mis)
                if (o instanceof String str)
                    imports.add(str);
        }
        Object entityImps = entityCtx.get("imports");
        if (entityImps instanceof Set<?> eis) {
            for (Object o : eis)
                if (o instanceof String str)
                    imports.add(str);
        }
        context.put("imports", imports);
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());

        return context;
    }

    private String resolveControllerInterfaceFilename(EntityDef entity, SddModel model) {
        var options = model.database().generatorOptions();
        var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
        return pkg.replace('.', '/') + "/" + contextBuilder.toPascal(entity.name()) + "Api.java";
    }

    private String resolveControllerFilename(EntityDef entity, SddModel model) {
        var options = model.database().generatorOptions();
        var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
        return pkg.replace('.', '/') + "/" + contextBuilder.toPascal(entity.name()) + "Controller.java";
    }
}
