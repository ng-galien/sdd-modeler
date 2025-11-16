package io.statemodeler.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import io.statemodeler.core.SddModel;

/**
 * Generates JSON Schema for SDD model YAML files using victools generator.
 * Enables IDE support with autocompletion and validation.
 *
 * Based on victools/jsonschema-generator documentation:
 * https://victools.github.io/jsonschema-generator
 */
public final class SddSchemaGenerator {

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
            JsonNode jsonSchema = generator.generateSchema(SddModel.class);

            // Enhance schema with metadata
            if (jsonSchema instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                var schemaObj = (com.fasterxml.jackson.databind.node.ObjectNode) jsonSchema;
                schemaObj.put("$id", "https://schemas.statemodeler.io/v1/sdd-model.json");
                schemaObj.put("title", "SDD Model Schema");
                schemaObj.put(
                        "description",
                        "JSON Schema for State-Driven Design (SDD) YAML model files. "
                                + "Enables IDE autocompletion and validation for SDD entities, states, transitions, and database configurations.");
            }

            // Pretty print for human readability
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate SDD JSON Schema", e);
        }
    }
}
