package io.statemodeler.codegen.java;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import io.statemodeler.core.StateDef;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JavaEntityGenerator extends JavaGeneratorBase {

    public JavaEntityGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        super(engine, contextBuilder);
    }

    @Override
    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        for (EntityDef entity : model.entities().values()) {
            String content = generateFile(entity, model, "templates/java/entity.java.pebble");
            String filename = resolveFilename(model, contextBuilder.toPascal(entity.name()) + "State");
            generatedFiles.put(filename, content);

            String idContent = generateFile(entity, model, "templates/java/id.java.pebble");
            String idFilename = resolveFilename(model, contextBuilder.toPascal(entity.name()) + "Id");
            generatedFiles.put(idFilename, idContent);

            String domainStateContent = generateFile(entity, model, "templates/java/domain_state_entity.java.pebble");
            String domainStateFilename =
                    resolveFilename(model, contextBuilder.toPascal(entity.name()) + "DomainState");
            generatedFiles.put(domainStateFilename, domainStateContent);

            // Generate source table records for from_any_of transitions
            for (StateDef state : entity.states().values()) {
                if (!state.fromAnyOf().isEmpty()) {
                    String sourceTableContent = generateSourceTableFile(entity, state, model);
                    String sourceTableFilename =
                            resolveFilename(model, contextBuilder.toPascal(state.name()) + "Source");
                    generatedFiles.put(sourceTableFilename, sourceTableContent);
                }
            }
        }
        return generatedFiles;
    }

    private String generateSourceTableFile(EntityDef entity, StateDef state, SddModel model) {
        PebbleTemplate template = engine.getTemplate("templates/java/source_table.java.pebble");
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = contextBuilder.buildEntityContext(entity);
        Map<String, Object> stateCtx = contextBuilder.buildStateContext(state, entityCtx);
        context.put("entity", entityCtx);
        context.put("state", stateCtx);

        Set<String> imports = new HashSet<>();
        context.put("imports", imports);
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate source table for " + state.name() + " in " + entity.name(), e);
        }
    }
}
