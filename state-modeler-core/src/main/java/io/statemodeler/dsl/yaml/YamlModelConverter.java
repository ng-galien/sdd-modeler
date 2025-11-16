package io.statemodeler.dsl.yaml;

import io.statemodeler.core.*;
import io.statemodeler.dsl.yaml.YamlModelDto.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts YamlModelDto to SddModel.
 * Handles the mapping from YAML structure with dynamic keys to immutable records.
 */
public final class YamlModelConverter {

    private YamlModelConverter() {
        // Utility class
    }

    /**
     * Convert YamlModelDto to SddModel.
     */
    public static SddModel convert(YamlModelDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("YamlModelDto cannot be null");
        }

        var database = convertDatabase(dto.database());
        var entities = convertEntities(dto.entities());

        return new SddModel(dto.version(), dto.name(), database, entities);
    }

    private static DatabaseConfig convertDatabase(YamlDatabaseDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Database configuration is required");
        }
        return new DatabaseConfig(dto.dialect(), dto.schema(), dto.stateSchema());
    }

    private static Map<String, EntityDef> convertEntities(Map<String, YamlEntityDto> entities) {
        if (entities == null) {
            return Map.of();
        }

        return entities.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> convertEntity(entry.getKey(), entry.getValue())));
    }

    private static EntityDef convertEntity(String entityName, YamlEntityDto dto) {
        var id = convertAttribute(dto.id());
        var attributes = convertAttributes(dto.attributes());
        var states = convertStates(dto.states());
        var extensions = convertExtensions(dto.extensions());
        var projections = convertProjections(dto.projections());

        return new EntityDef(entityName, dto.table(), id, attributes, states, extensions, projections);
    }

    private static AttributeDef convertAttribute(YamlAttributeDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Attribute definition is required");
        }

        var name = dto.name() != null ? dto.name() : "id";
        var nullable = dto.nullable() != null ? dto.nullable() : false;
        var primaryKey = dto.primaryKey() != null ? dto.primaryKey() : false;

        return new AttributeDef(name, dto.type(), nullable, primaryKey, dto.defaultValue(), dto.description());
    }

    private static Map<String, AttributeDef> convertAttributes(Map<String, YamlAttributeDto> attributes) {
        if (attributes == null) {
            return Map.of();
        }

        return attributes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            var dto = entry.getValue();
            var name = entry.getKey(); // Use the map key as the attribute name
            var nullable = dto.nullable() != null ? dto.nullable() : true;
            var primaryKey = dto.primaryKey() != null ? dto.primaryKey() : false;
            return new AttributeDef(name, dto.type(), nullable, primaryKey, dto.defaultValue(), dto.description());
        }));
    }

    private static Map<String, StateDef> convertStates(Map<String, YamlStateDto> states) {
        if (states == null) {
            return Map.of();
        }

        return states.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> convertState(entry.getKey(), entry.getValue())));
    }

    private static StateDef convertState(String stateName, YamlStateDto dto) {
        var initial = dto.initial() != null ? dto.initial() : false;
        var from = dto.from() != null ? List.copyOf(dto.from()) : List.<String>of();
        var fromAnyOf = dto.fromAnyOf() != null ? List.copyOf(dto.fromAnyOf()) : List.<String>of();
        var attributes = convertAttributes(dto.attributes());

        return new StateDef(stateName, dto.table(), initial, from, fromAnyOf, attributes);
    }

    private static Map<String, ExtensionDef> convertExtensions(Map<String, YamlExtensionDto> extensions) {
        if (extensions == null) {
            return Map.of();
        }

        return extensions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> convertExtension(entry.getKey(), entry.getValue())));
    }

    private static ExtensionDef convertExtension(String extensionName, YamlExtensionDto dto) {
        var attributes = convertAttributes(dto.attributes());
        return new ExtensionDef(extensionName, dto.table(), dto.targetState(), attributes);
    }

    private static Map<String, ProjectionDef> convertProjections(Map<String, YamlProjectionDto> projections) {
        if (projections == null) {
            return Map.of();
        }

        return projections.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> convertProjection(entry.getKey(), entry.getValue())));
    }

    private static ProjectionDef convertProjection(String projectionName, YamlProjectionDto dto) {
        var kind = ProjectionDef.ProjectionKind.valueOf(dto.kind().toUpperCase());
        return new ProjectionDef(projectionName, dto.name(), kind);
    }
}
