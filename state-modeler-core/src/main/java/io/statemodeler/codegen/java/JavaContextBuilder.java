package io.statemodeler.codegen.java;

import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import io.statemodeler.core.StateDef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.text.CaseUtils;

public class JavaContextBuilder {

    public Map<String, Object> buildModelContext(SddModel model) {
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

    public Map<String, Object> buildEntityContext(EntityDef entity) {
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

        List<Map<String, Object>> attrs = new ArrayList<>();
        for (var attr : entity.attributes().values()) {
            Map<String, Object> attrCtx = new HashMap<>();
            attrCtx.put("name", attr.name());
            attrCtx.put("propertyName", toCamel(attr.name()));
            var mapped = mapSqlTypeToJavaType(attr.type());
            attrCtx.put("javaType", mapped.javaType());
            if (mapped.importName() != null)
                imports.add(mapped.importName());
            attrs.add(attrCtx);
        }
        ctx.put("attributes", attrs);
        ctx.put("imports", imports);
        ctx.put("hasStates", !entity.states().isEmpty());
        return ctx;
    }

    public Map<String, Object> buildStateContext(StateDef state, Map<String, Object> entityCtx) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("name", state.name());
        ctx.put("className", toPascal(state.name()));
        ctx.put("propertyName", toCamel(state.name()));
        List<Map<String, Object>> attrs = new ArrayList<>();
        Set<String> imports = new HashSet<>();
        for (var attr : state.attributes().values()) {
            Map<String, Object> attrCtx = new HashMap<>();
            attrCtx.put("name", attr.name());
            attrCtx.put("propertyName", toCamel(attr.name()));
            attrCtx.put("nullable", attr.nullable());
            var mapped = mapSqlTypeToJavaType(attr.type());
            attrCtx.put("javaType", mapped.javaType());
            if (mapped.importName() != null)
                imports.add(mapped.importName());
            attrs.add(attrCtx);
        }
        ctx.put("attributes", attrs);
        ctx.put("imports", imports);

        List<Map<String, String>> fromStates = new ArrayList<>();
        for (String from : state.from()) {
            Map<String, String> fromCtx = new HashMap<>();
            fromCtx.put("name", from);
            fromCtx.put("className", toPascal(from));
            fromCtx.put("propertyName", toCamel(from));
            fromStates.add(fromCtx);
        }
        ctx.put("from", fromStates);

        return ctx;
    }

    public record TypeMapping(String javaType, String importName) {
    }

    public TypeMapping mapSqlTypeToJavaType(String sqlType) {
        if (sqlType == null)
            return new TypeMapping("Object", null);
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

    public String toPascal(String s) {
        if (s == null || s.isEmpty())
            return s;
        return CaseUtils.toCamelCase(normalizeCaseInput(s), true, '_', '-', ' ', '.');
    }

    public String toCamel(String s) {
        if (s == null || s.isEmpty())
            return s;
        return CaseUtils.toCamelCase(normalizeCaseInput(s), false, '_', '-', ' ', '.');
    }

    private String normalizeCaseInput(String s) {
        // Normalize existing camel/pascal case boundaries so CaseUtils keeps word
        // breaks
        return s.replaceAll("(?<=[A-Za-z0-9])(?=[A-Z])", "_");
    }

    /**
     * Build context for all transitions of an entity.
     * Each transition context includes:
     * - sourceStates: list of state names that can transition to the target
     * - targetState: the target state name, className, propertyName
     * - methodName: the service method name (transitionTo{TargetState})
     * - commandClassName: the command record class name
     * - commandFields: list of field contexts from target state attributes
     */
    public List<Map<String, Object>> buildTransitionsContext(EntityDef entity) {
        List<Map<String, Object>> transitions = new ArrayList<>();

        // For each state (except initial states), create a transition context
        for (StateDef targetState : entity.states().values()) {
            // Skip initial states (they have no 'from' transitions)
            if (targetState.from().isEmpty() && targetState.fromAnyOf().isEmpty()) {
                continue;
            }

            Map<String, Object> transitionCtx = new HashMap<>();

            // Target state info
            Map<String, String> targetCtx = new HashMap<>();
            targetCtx.put("name", targetState.name());
            targetCtx.put("className", toPascal(targetState.name()));
            targetCtx.put("propertyName", toCamel(targetState.name()));
            transitionCtx.put("targetState", targetCtx);

            // Source states (from regular 'from' or 'fromAnyOf')
            List<Map<String, String>> sources = new ArrayList<>();
            for (String from : targetState.from()) {
                Map<String, String> sourceCtx = new HashMap<>();
                sourceCtx.put("name", from);
                sourceCtx.put("className", toPascal(from));
                sourceCtx.put("repositoryName", toPascal(from) + "Repository");
                sources.add(sourceCtx);
            }
            for (String from : targetState.fromAnyOf()) {
                Map<String, String> sourceCtx = new HashMap<>();
                sourceCtx.put("name", from);
                sourceCtx.put("className", toPascal(from));
                sourceCtx.put("repositoryName", toPascal(from) + "Repository");
                sources.add(sourceCtx);
            }
            transitionCtx.put("sourceStates", sources);

            // Method name and command class name
            String methodName = "transitionTo" + toPascal(targetState.name());
            transitionCtx.put("methodName", methodName);
            transitionCtx.put("commandClassName", "TransitionTo" + toPascal(targetState.name()) + "Command");

            // Command fields (from target state attributes)
            List<Map<String, Object>> commandFields = new ArrayList<>();
            for (var attr : targetState.attributes().values()) {
                Map<String, Object> fieldCtx = new HashMap<>();
                fieldCtx.put("name", attr.name());
                fieldCtx.put("propertyName", toCamel(attr.name()));
                fieldCtx.put("nullable", attr.nullable());
                var mapped = mapSqlTypeToJavaType(attr.type());
                fieldCtx.put("javaType", mapped.javaType());
                commandFields.add(fieldCtx);
            }
            transitionCtx.put("commandFields", commandFields);

            transitions.add(transitionCtx);
        }

        return transitions;
    }
}
