package io.statemodeler.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import io.statemodeler.dsl.yaml.YamlModelDto;

/**
 * Generates JSON Schema for SDD model YAML files using victools generator.
 * Enables IDE support with autocompletion and validation.
 *
 * Based on victools/jsonschema-generator documentation:
 * https://victools.github.io/jsonschema-generator
 */
public final class SddSchemaGenerator {

    private static final String SEMVER_PATTERN =
            "^(0|[1-9]\\\\d*)\\\\.(0|[1-9]\\\\d*)\\\\.(0|[1-9]\\\\d*)(?:-[0-9A-Za-z.-]+)?(?:\\\\+[0-9A-Za-z.-]+)?$";

    // Matches PostgreSQL types accepted by PostgresTypeValidator, including array forms (case-insensitive).
    private static final String POSTGRES_TYPE_PATTERN =
            "^(?i)((SMALLINT|INTEGER|INT|BIGINT|REAL|DOUBLE PRECISION|SMALLSERIAL|SERIAL|BIGSERIAL|"
                    + "DECIMAL(\\\\(\\\\d+(,\\\\s*\\\\d+)?\\\\))?|NUMERIC(\\\\(\\\\d+(,\\\\s*\\\\d+)?\\\\))?|"
                    + "CHAR(\\\\(\\\\d+\\\\))?|VARCHAR(\\\\(\\\\d+\\\\))?|TEXT|BYTEA|"
                    + "TIMESTAMP(\\\\(\\\\d+\\\\))?(\\\\s+(WITH|WITHOUT)\\\\s+TIME\\\\s+ZONE)?|"
                    + "TIMESTAMPTZ(\\\\(\\\\d+\\\\))?|"
                    + "TIME(\\\\(\\\\d+\\\\))?(\\\\s+(WITH|WITHOUT)\\\\s+TIME\\\\s+ZONE)?|"
                    + "TIMETZ(\\\\(\\\\d+\\\\))?|DATE|INTERVAL|BOOLEAN|BOOL|JSONB?|UUID))(\\\\[\\\\])?$";

    /**
     * Generate complete JSON Schema for SDD model YAML files.
     * @return JSON Schema as a JSON string
     */
    public String generateSchema() {
        try {
            // Configure Jackson module for proper JSON/YAML parsing support
            JacksonModule jacksonModule = new JacksonModule();

            // Use PLAIN_JSON preset for IDE compatibility with additional options
            SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(
                            SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                    .with(jacksonModule)
                    .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                    .with(Option.DEFINITIONS_FOR_ALL_OBJECTS) // Generate $defs for better IDE support
                    .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES) // Expand Map<String,EntityDef> value types
                    .with(Option.FLATTENED_ENUMS) // Show enums as string + enum values
                    .with(Option.PUBLIC_NONSTATIC_FIELDS) // Include public fields
                    .without(Option.FLATTENED_OPTIONALS) // Keep Optional types detailed
                    .build();

            SchemaGenerator generator = new SchemaGenerator(config);
            JsonNode jsonSchema = generator.generateSchema(YamlModelDto.class);

            if (jsonSchema instanceof ObjectNode schemaObj) {
                schemaObj.put("$id", "https://schemas.statemodeler.io/v1/sdd-model.json");
                schemaObj.put("title", "SDD Model Schema");
                schemaObj.put(
                        "description",
                        "JSON Schema for State-Driven Design (SDD) YAML model files. "
                                + "Enables IDE autocompletion and validation for SDD entities, states, transitions, and database configurations.");

                applyRequiredAndConstraints(schemaObj);
            }

            // Pretty print for human readability
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate SDD JSON Schema", e);
        }
    }

    private static void applyRequiredAndConstraints(ObjectNode schemaObj) {
        // Root requirements
        addRequired(schemaObj, "version", "name", "database", "entities");
        schemaObj.put("additionalProperties", false);
        setSemver(schemaObj);

        var defs = (ObjectNode) schemaObj.get("$defs");
        if (defs == null) {
            return;
        }

        constrainDatabase(defs);
        constrainEntity(defs);
        constrainState(defs);
        constrainAttribute(defs);
        constrainExtension(defs);
        constrainProjection(defs);
    }

    private static void constrainDatabase(ObjectNode defs) {
        var db = (ObjectNode) defs.get("YamlModelDto.YamlDatabaseDto");
        if (db == null) return;
        addRequired(db, "dialect");
        db.put("additionalProperties", false);
        var props = (ObjectNode) db.get("properties");
        if (props != null && props.has("dialect")) {
            ((ObjectNode) props.get("dialect")).putArray("enum").add("postgres");
        }
    }

    private static void constrainEntity(ObjectNode defs) {
        var entity = (ObjectNode) defs.get("YamlModelDto.YamlEntityDto");
        if (entity == null) return;
        addRequired(entity, "table", "id", "states", "name");
        entity.put("additionalProperties", false);
    }

    private static void constrainState(ObjectNode defs) {
        var state = (ObjectNode) defs.get("YamlModelDto.YamlStateDto");
        if (state == null) return;
        addRequired(state, "table", "attributes");
        state.put("additionalProperties", false);

        var props = (ObjectNode) state.get("properties");
        if (props != null) {
            if (props.has("from_any_of")) {
                ((ObjectNode) props.get("from_any_of")).put("minItems", 2);
            }
            if (props.has("from")) {
                ((ObjectNode) props.get("from")).put("type", "string");
            }
        }
    }

    private static void constrainAttribute(ObjectNode defs) {
        var attr = (ObjectNode) defs.get("YamlModelDto.YamlAttributeDto");
        if (attr == null) return;
        addRequired(attr, "type", "name");
        attr.put("additionalProperties", false);

        var props = (ObjectNode) attr.get("properties");
        if (props != null && props.has("type")) {
            ((ObjectNode) props.get("type")).put("pattern", POSTGRES_TYPE_PATTERN);
        }
    }

    private static void constrainExtension(ObjectNode defs) {
        var ext = (ObjectNode) defs.get("YamlModelDto.YamlExtensionDto");
        if (ext == null) return;
        addRequired(ext, "table", "target_state", "attributes");
        ext.put("additionalProperties", false);
    }

    private static void constrainProjection(ObjectNode defs) {
        var proj = (ObjectNode) defs.get("YamlModelDto.YamlProjectionDto");
        if (proj == null) return;
        addRequired(proj, "name", "view_name", "kind");
        proj.put("additionalProperties", false);
    }

    private static void setSemver(ObjectNode schemaObj) {
        var props = (ObjectNode) schemaObj.get("properties");
        if (props != null && props.has("version")) {
            ((ObjectNode) props.get("version")).put("pattern", SEMVER_PATTERN);
        }
    }

    private static void addRequired(ObjectNode node, String... requiredFields) {
        ArrayNode required = node.withArray("required");
        for (String field : requiredFields) {
            required.add(field);
        }
    }
}
