package io.statemodeler.dsl.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * DTO for parsing YAML/JSON structure before converting to SddModel.
 * Handles the dynamic key structure of the YAML format.
 */
public record YamlModelDto(String version, String name, YamlDatabaseDto database, Map<String, YamlEntityDto> entities) {

    public record YamlDatabaseDto(
            String dialect,
            String schema,
            @JsonProperty("state_schema") String stateSchema,
            @JsonProperty("generator_options") Map<String, String> generatorOptions) {}

    public record YamlEntityDto(
            String table,
            YamlAttributeDto id,
            Map<String, YamlAttributeDto> attributes,
            Map<String, YamlStateDto> states,
            Map<String, YamlExtensionDto> extensions,
            Map<String, YamlProjectionDto> projections) {}

    public record YamlAttributeDto(
            String name,
            String type,
            Boolean nullable,
            @JsonProperty("primary_key") Boolean primaryKey,
            @JsonProperty("default") String defaultValue,
            String description) {}

    public record YamlStateDto(
            Boolean initial,
            String table,
            String from,
            @JsonProperty("from_any_of") List<String> fromAnyOf,
            Map<String, YamlAttributeDto> attributes) {}

    public record YamlExtensionDto(
            String table, @JsonProperty("target_state") String targetState, Map<String, YamlAttributeDto> attributes) {}

    public record YamlProjectionDto(String kind, String name) {}
}
