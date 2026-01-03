package io.statemodeler.sql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Utility to wrap generated SQL into a Liquibase YAML changelog.
 * Shared by Gradle and Maven plugins to avoid duplication.
 */
public final class LiquibaseYamlRenderer {

    private LiquibaseYamlRenderer() {}

    public static String render(String sql) {
        String id = UUID.randomUUID().toString();

        Map<String, Object> sqlChange = new LinkedHashMap<>();
        sqlChange.put("splitStatements", false);
        sqlChange.put("stripComments", false);
        sqlChange.put("sql", sql);

        Map<String, Object> change = new LinkedHashMap<>();
        change.put("sql", sqlChange);

        Map<String, Object> changeSet = new LinkedHashMap<>();
        changeSet.put("id", id);
        changeSet.put("author", "sdd-modeler");
        changeSet.put("changes", List.of(change));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("databaseChangeLog", List.of(changeSet));

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(2);
        options.setWidth(120);

        return new Yaml(options).dump(root);
    }
}
