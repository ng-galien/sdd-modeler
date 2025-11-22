package io.statemodeler.sql.postgres;

import io.statemodeler.core.EntityDef;
import io.statemodeler.sql.ConstraintDefinition;
import io.statemodeler.sql.IndexDefinition;
import java.util.List;

/**
 * Generates PostgreSQL index definitions for foreign keys and other columns.
 *
 * <p>This class is responsible for creating index definitions that improve query performance,
 * particularly for foreign key columns used in joins and lookups. It supports both simple and
 * composite foreign keys.
 *
 * <h2>Index Naming Convention</h2>
 * Indexes are named using the pattern: {@code idx_<table>_<col1>_<col2>_...}
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * var generator = new PostgresIndexGenerator();
 * var fk = new ConstraintDefinition("fk_order_customer", "orders", "FOREIGN KEY (customer_id) REFERENCES customers(id)");
 * var index = generator.generateIndexForForeignKey(fk, entity, "orders");
 * // Result: IndexDefinition("idx_orders_customer_id", "orders", "public", ["customer_id"], false)
 * }</pre>
 */
final class PostgresIndexGenerator {

    /**
     * Generates an index definition for a foreign key constraint.
     *
     * <p>The method parses the foreign key definition to extract column names and creates an index
     * on those columns to improve join performance. Supports both simple and composite foreign
     * keys.
     *
     * @param fk the foreign key constraint definition (e.g., "FOREIGN KEY (col1, col2) REFERENCES
     *     ...")
     * @param entity the entity definition (used for schema context)
     * @param tableName the table name where the index will be created
     * @return an {@link IndexDefinition} for the foreign key columns
     * @throws StringIndexOutOfBoundsException if the FK definition is malformed (missing
     *     parentheses)
     */
    IndexDefinition generateIndexForForeignKey(ConstraintDefinition fk, EntityDef entity, String tableName) {
        // Extract column names from FK definition: "FOREIGN KEY (col1, col2) REFERENCES ..."
        // Supports both simple and composite foreign keys
        var fkDef = fk.definition();
        var startIdx = fkDef.indexOf('(') + 1;
        var endIdx = fkDef.indexOf(')');
        var columnsPart = fkDef.substring(startIdx, endIdx).trim();

        // Split by comma and trim each column name
        var columns = List.of(columnsPart.split(",")).stream().map(String::trim).toList();

        // Generate index name: idx_tablename_col1_col2
        var indexName = "idx_" + tableName + "_" + String.join("_", columns);
        var schema =
                fk.table().contains(".") ? fk.table().substring(0, fk.table().indexOf('.')) : "public";

        return new IndexDefinition(indexName, tableName, schema, columns, false);
    }

    // Index DDL rendering is handled by Pebble templates inside the
    // PebblePostgresDdlGenerator; remove legacy string-based rendering.
}
