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

public class JavaControllerGenerator extends JavaGeneratorBase {

    public JavaControllerGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        super(engine, contextBuilder);
    }

    @Override
    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        for (EntityDef entity : model.entities().values()) {
            // Generate interface
            String interfaceContent = generateControllerInterface(entity, model);
            String interfaceFilename = resolveFilename(model, contextBuilder.toPascal(entity.name()) + "Api");
            generatedFiles.put(interfaceFilename, interfaceContent);

            // Generate implementation
            String implContent = generateControllerImplementation(entity, model);
            String implFilename = resolveFilename(model, contextBuilder.toPascal(entity.name()) + "Controller");
            generatedFiles.put(implFilename, implContent);
        }
        return generatedFiles;
    }

    private String generateControllerInterface(EntityDef entity, SddModel model) {
        return generateFileWithContext(entity, model, "templates/java/controller_interface.java.pebble");
    }

    private String generateControllerImplementation(EntityDef entity, SddModel model) {
        return generateFileWithContext(entity, model, "templates/java/controller.java.pebble");
    }

    private String generateFileWithContext(EntityDef entity, SddModel model, String templatePath) {
        PebbleTemplate template = engine.getTemplate(templatePath);
        Map<String, Object> context = buildContext(entity, model);

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Controller for " + entity.name(), e);
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
                // include the property name for the entity id (e.g., leadId) used in state
                // records
                Map<String, Object> stateCtx = contextBuilder.buildStateContext(state, entityCtx);
                Object entIdProp = stateCtx.get("entityIdPropertyName");
                if (entIdProp instanceof String)
                    stateRepo.put("entityIdPropertyName", (String) entIdProp);
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
}
