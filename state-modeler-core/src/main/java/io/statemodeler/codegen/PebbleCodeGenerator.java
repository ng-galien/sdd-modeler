package io.statemodeler.codegen;

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
import org.apache.commons.text.CaseUtils;

/**
 * Pebble-based code generator implementation. Uses templates stored under
 * src/main/resources/templates.
 */
public class PebbleCodeGenerator implements CodeGenerator {

    private final PebbleEngine engine;
    private final String language;

    public PebbleCodeGenerator(String language) {
        this.engine = new PebbleEngine.Builder().build();
        this.language = language;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();

        // Example: Generate a file per entity
        for (EntityDef entity : model.entities().values()) {
            String content = generateEntity(entity, model);
            String filename = resolveFilename(entity, model);
            generatedFiles.put(filename, content);

            if ("java".equals(language)) {
                // Generate ID Value Object
                String idContent = generateId(entity, model);
                String idFilename = resolveIdFilename(entity, model);
                generatedFiles.put(idFilename, idContent);

                // Generate Converters
                String convertersContent = generateConverters(entity, model);
                String convertersFilename = resolveConvertersFilename(entity, model);
                generatedFiles.put(convertersFilename, convertersContent);

                for (StateDef state : entity.states().values()) {
                    String repoContent = generateRepository(entity, state, model);
                    String repoFilename = resolveRepositoryFilename(entity, state, model);
                    generatedFiles.put(repoFilename, repoContent);
                }
            }
        }

        if ("java".equals(language)) {
            // Generate Configuration
            String configContent = generateConfiguration(model);
            String configFilename = resolveConfigurationFilename(model);
            generatedFiles.put(configFilename, configContent);
        }

        return generatedFiles;
    }

    private String generateEntity(EntityDef entity, SddModel model) {
        String templatePath = "templates/" + language + "/entity." + getExtension() + ".pebble";
        PebbleTemplate template = engine.getTemplate(templatePath);
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = buildEntityContext(entity);
        context.put("entity", entityCtx);
        Map<String, Object> modelCtx = buildModelContext(model);
        context.put("model", modelCtx);
        // Merge imports for the top of the file: model-level imports + entity-level imports
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
            throw new RuntimeException("Failed to generate entity " + entity.name(), e);
        }
    }

    private String getExtension() {
        return switch (language) {
            case "java" -> "java";
            case "python" -> "py";
            default -> "txt";
        };
    }

    private String resolveFilename(EntityDef entity, SddModel model) {
        if ("java".equals(language)) {
            var options = model.database().generatorOptions();
            var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
            // Ensure the Java file name matches the public type name emitted by the template
            return pkg.replace('.', '/') + "/" + toPascal(entity.name()) + "State.java";
        }
        return entity.name() + "." + getExtension();
    }

    private String generateRepository(EntityDef entity, StateDef state, SddModel model) {
        // repository template path computed in generateFromTemplate
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = buildEntityContext(entity);
        context.put("entity", entityCtx);
        context.put("state", buildStateContext(state, entityCtx));
        context.put("model", buildModelContext(model));
        // Place top-level imports into context for repository files as well
        Map<String, Object> modelCtx = buildModelContext(model);
        Object modelImps = modelCtx.get("imports");
        Set<String> imports = new HashSet<>();
        if (modelImps instanceof Set<?> mis) {
            for (Object o : mis) if (o instanceof String str) imports.add(str);
        }
        context.put("imports", imports);
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());
        return generateFromTemplate("repository", context);
    }

    private String resolveRepositoryFilename(EntityDef entity, StateDef state, SddModel model) {
        if ("java".equals(language)) {
            var options = model.database().generatorOptions();
            var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
            return pkg.replace('.', '/') + "/" + toPascal(state.name()) + "Repository.java";
        }
        return state.name() + "Repository." + getExtension();
    }

    private String generateId(EntityDef entity, SddModel model) {
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = buildEntityContext(entity);
        context.put("entity", entityCtx);
        Map<String, Object> modelCtx = buildModelContext(model);
        context.put("model", modelCtx);
        // Merge imports for id template
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
        // Ensure templates have generator options (packageName, etc.) available
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());
        return generateFromTemplate("id", context);
    }

    private String resolveIdFilename(EntityDef entity, SddModel model) {
        var options = model.database().generatorOptions();
        var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
        return pkg.replace('.', '/') + "/" + toPascal(entity.name()) + "Id.java";
    }

    private String generateConverters(EntityDef entity, SddModel model) {
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> entityCtx = buildEntityContext(entity);
        context.put("entity", entityCtx);
        Map<String, Object> modelCtx = buildModelContext(model);
        context.put("model", modelCtx);
        // Merge imports for converters
        Set<String> imports = new HashSet<>();
        Object modelImps = modelCtx.get("imports");
        if (modelImps instanceof Set<?> mis) {
            for (Object o : mis) if (o instanceof String str) imports.add(str);
        }
        Object entityImps2 = entityCtx.get("imports");
        if (entityImps2 instanceof Set<?> eis2) {
            for (Object o : eis2) if (o instanceof String str) imports.add(str);
        }
        context.put("imports", imports);
        // Provide generator options (like packageName) to converters template
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());
        return generateFromTemplate("converters", context);
    }

    private String resolveConvertersFilename(EntityDef entity, SddModel model) {
        var options = model.database().generatorOptions();
        var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
        return pkg.replace('.', '/') + "/" + toPascal(entity.name()) + "Converters.java";
    }

    private String generateConfiguration(SddModel model) {
        Map<String, Object> context = new HashMap<>();
        context.put("model", buildModelContext(model));
        context.put("options", model.database() != null ? model.database().generatorOptions() : Map.of());
        return generateFromTemplate("configuration", context);
    }

    private String resolveConfigurationFilename(SddModel model) {
        var options = model.database().generatorOptions();
        var pkg = options != null ? options.getOrDefault("packageName", "com.example") : "com.example";
        return pkg.replace('.', '/') + "/SddConfig.java";
    }

    private String generateFromTemplate(String templateName, Map<String, Object> context) {
        String templatePath = "templates/" + language + "/" + templateName + "." + getExtension() + ".pebble";
        PebbleTemplate template = engine.getTemplate(templatePath);
        if (context == null) context = new HashMap<>();
        context.putIfAbsent("options", Map.of());

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate " + templateName, e);
        }
    }

    // Helper to compute model context for templates, including entity wrappers and imports
    private Map<String, Object> buildModelContext(SddModel model) {
        Map<String, Object> modelCtx = new HashMap<>();
        List<Map<String, Object>> entities = new ArrayList<>();
        Set<String> imports = new HashSet<>();
        for (EntityDef e : model.entities().values()) {
            Map<String, Object> entityCtx = buildEntityContext(e);
            entities.add(entityCtx);
            Object impsObj = entityCtx.get("imports");
            if (impsObj instanceof Set<?> imps) {
                for (Object o : imps) {
                    if (o instanceof String str) {
                        imports.add(str);
                    }
                }
            }
        }
        modelCtx.put("entities", entities);
        modelCtx.put("imports", imports);
        return modelCtx;
    }

    private Map<String, Object> buildEntityContext(EntityDef entity) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("name", entity.name());
        String className = toPascal(entity.name());
        ctx.put("className", className);
        ctx.put("table", entity.table() != null ? entity.table() : entity.name());
        Map<String, Object> idCtx = new HashMap<>();
        idCtx.put("name", entity.id().name());
        idCtx.put("propertyName", toCamel(entity.id().name()));
        idCtx.put("className", className + "Id");
        ctx.put("id", idCtx);

        Set<String> imports = new HashSet<>();
        List<Map<String, Object>> states = new ArrayList<>();
        for (StateDef s : entity.states().values()) {
            Map<String, Object> stateCtx = buildStateContext(s, ctx);
            states.add(stateCtx);
            Object impsObj = stateCtx.get("imports");
            if (impsObj instanceof Set<?> imps) {
                for (Object o : imps) {
                    if (o instanceof String str) {
                        imports.add(str);
                    }
                }
            }
        }
        ctx.put("states", states);
        ctx.put("imports", imports);
        return ctx;
    }

    private Map<String, Object> buildStateContext(StateDef state, Map<String, Object> entityCtx) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("name", state.name());
        ctx.put("className", toPascal(state.name()));
        List<Map<String, Object>> attrs = new ArrayList<>();
        Set<String> imports = new HashSet<>();
        for (var attr : state.attributes().values()) {
            Map<String, Object> attrCtx = new HashMap<>();
            attrCtx.put("name", attr.name());
            attrCtx.put("propertyName", toCamel(attr.name()));
            var mapped = mapSqlTypeToJavaType(attr.type());
            attrCtx.put("javaType", mapped.javaType());
            if (mapped.importName() != null) imports.add(mapped.importName());
            attrs.add(attrCtx);
        }
        ctx.put("attributes", attrs);
        ctx.put("imports", imports);
        return ctx;
    }

    record TypeMapping(String javaType, String importName) {}

    private TypeMapping mapSqlTypeToJavaType(String sqlType) {
        if (sqlType == null) return new TypeMapping("Object", null);
        var s = sqlType.trim().toUpperCase();
        boolean isArray = false;
        if (s.endsWith("[]")) {
            isArray = true;
            s = s.substring(0, s.length() - 2).trim();
        }
        String javaType = "Object";
        String importName = null;
        if (s.startsWith("VARCHAR") || s.startsWith("CHAR") || s.equals("TEXT")) {
            javaType = "String";
        } else if (s.startsWith("NUMERIC") || s.startsWith("DECIMAL")) {
            javaType = "BigDecimal";
            importName = "java.math.BigDecimal";
        } else if (s.equals("SMALLINT") || s.equals("INT") || s.equals("INTEGER")) {
            javaType = "Integer";
        } else if (s.equals("BIGINT")) {
            javaType = "Long";
        } else if (s.equals("REAL") || s.equals("DOUBLE PRECISION")) {
            javaType = "Double";
        } else if (s.equals("BOOLEAN") || s.equals("BOOL")) {
            javaType = "Boolean";
        } else if (s.equals("UUID")) {
            javaType = "UUID";
            importName = "java.util.UUID";
        } else if (s.startsWith("TIMESTAMP") || s.startsWith("TIMESTAMPTZ")) {
            javaType = "Instant";
            importName = "java.time.Instant";
        } else if (s.equals("DATE")) {
            javaType = "java.time.LocalDate";
            importName = "java.time.LocalDate";
        } else if (s.equals("TIME") || s.startsWith("TIMETZ")) {
            javaType = "java.time.LocalTime";
            importName = "java.time.LocalTime";
        } else if (s.equals("JSON") || s.equals("JSONB")) {
            javaType = "String";
        } else if (s.equals("BYTEA")) {
            javaType = "byte[]";
        }
        if (isArray) {
            // Prefer List<T> for arrays
            if (javaType.endsWith("[]")) {
                javaType = javaType.replace("[]", "");
            }
            javaType = "List<" + javaType + ">";
            importName = (importName == null) ? "java.util.List" : importName;
        }
        return new TypeMapping(javaType, importName);
    }

    private String toPascal(String s) {
        if (s == null || s.isEmpty()) return s;
        return CaseUtils.toCamelCase(normalizeCaseInput(s), true, '_', '-', ' ', '.');
    }

    private String toCamel(String s) {
        if (s == null || s.isEmpty()) return s;
        return CaseUtils.toCamelCase(normalizeCaseInput(s), false, '_', '-', ' ', '.');
    }

    private String normalizeCaseInput(String s) {
        // Normalize existing camel/pascal case boundaries so CaseUtils keeps word breaks
        return s.replaceAll("(?<=[A-Za-z0-9])(?=[A-Z])", "_");
    }
}
