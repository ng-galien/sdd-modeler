package io.statemodeler.gradle;

import java.util.UUID;

/**
 * Minimal renderer to wrap generated SQL into a Liquibase YAML changelog.
 * SQL is emitted as a single changeSet using sql-formatted block.
 */
public final class LiquibaseYamlRenderer {

    private LiquibaseYamlRenderer() {}

    public static String render(String sql) {
        String id = UUID.randomUUID().toString();
        // Liquibase YAML with inline SQL block.
        return "databaseChangeLog:\n" +
                "  - changeSet:\n" +
                "      id: " + id + "\n" +
                "      author: sdd-modeler\n" +
                "      changes:\n" +
                "        - sql:\n" +
                "            splitStatements: false\n" +
                "            stripComments: false\n" +
                "            sql: |\n" + indent(sql, 14, true) + "\n";
    }

    private static String indent(String text, int spaces, boolean includeFirstLine) {
        String padding = " ".repeat(spaces);
        String withFirst = includeFirstLine ? padding + text : text;
        return withFirst.replace("\n", "\n" + padding);
    }
}
