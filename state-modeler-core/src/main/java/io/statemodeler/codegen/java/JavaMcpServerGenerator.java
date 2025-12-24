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

public class JavaMcpServerGenerator extends JavaGeneratorBase {

    public JavaMcpServerGenerator(PebbleEngine engine, JavaContextBuilder contextBuilder) {
        super(engine, contextBuilder);
    }

    @Override
    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        for (EntityDef entity : model.entities().values()) {
            String content = generateMcpServer(entity, model);
            String filename = resolveFilename(model, contextBuilder.toPascal(entity.name()) + "McpServer");
            generatedFiles.put(filename, content);
        }
        return generatedFiles;
    }

    private String generateMcpServer(EntityDef entity, SddModel model) {
        PebbleTemplate template = engine.getTemplate("templates/java/mcp_server.java.pebble");
        Map<String, Object> context = buildContext(entity, model);
        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate MCP server for " + entity.name(), e);
        }
    }

    private Map<String, Object> buildContext(EntityDef entity, SddModel model) {
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = contextBuilder.buildEntityContext(entity);
        context.put("entity", entityCtx);
        Map<String, Object> modelCtx = contextBuilder.buildModelContext(model);
        context.put("model", modelCtx);

        List<Map<String, Object>> transitions = contextBuilder.buildTransitionsContext(entity);
        context.put("transitions", transitions);

        List<Map<String, String>> stateRepositories = new ArrayList<>();
        for (StateDef state : entity.states().values()) {
            Map<String, String> stateRepo = new HashMap<>();
            stateRepo.put("stateName", state.name());
            stateRepo.put("className", contextBuilder.toPascal(state.name()));
            stateRepo.put("repositoryName", contextBuilder.toPascal(state.name()) + "Repository");
            stateRepo.put("propertyName", contextBuilder.toCamel(state.name()));
            Map<String, Object> stateCtx = contextBuilder.buildStateContext(state, entityCtx);
            Object entIdProp = stateCtx.get("entityIdPropertyName");
            if (entIdProp instanceof String) {
                stateRepo.put("entityIdPropertyName", (String) entIdProp);
            }
            stateRepositories.add(stateRepo);
        }
        context.put("stateRepositories", stateRepositories);

        Set<String> imports = new HashSet<>();
        Object modelImps = modelCtx.get("imports");
        if (modelImps instanceof java.util.Collection<?> mis) {
            for (Object o : mis) {
                if (o instanceof String str) {
                    imports.add(str);
                }
            }
        }
        Object entityImps = entityCtx.get("imports");
        if (entityImps instanceof java.util.Collection<?> eis) {
            for (Object o : eis) {
                if (o instanceof String str) {
                    imports.add(str);
                }
            }
        }
        context.put("imports", imports);
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());
        context.put("codegen", model.database() != null ? model.database().codegenConfig() : null);

        return context;
    }
}
