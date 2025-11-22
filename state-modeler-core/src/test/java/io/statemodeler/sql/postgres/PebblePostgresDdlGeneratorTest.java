package io.statemodeler.sql.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import io.statemodeler.core.AttributeDef;
import io.statemodeler.core.DatabaseConfig;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import io.statemodeler.core.StateDef;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PebblePostgresDdlGeneratorTest {

    @Test
    void shouldGenerateSameOutputAsLegacyGenerator() {
        // Given
        var model = createSimpleOrderModel();
        var legacyGenerator = new PostgresDdlGenerator();
        var pebbleGenerator = new PebblePostgresDdlGenerator();

        // When
        var legacyDdl = legacyGenerator.generateDdl(model);
        var pebbleDdl = pebbleGenerator.generateDdl(model);

        // Then
        // Normalize using SQL Formatter for both outputs
        String normLegacy = SqlFormatter.of(Dialect.PostgreSql).format(legacyDdl);
        String normPebble = SqlFormatter.of(Dialect.PostgreSql).format(pebbleDdl);

        if (!normLegacy.equals(normPebble)) {
            System.out.println("LEGACY:\n" + legacyDdl);
            System.out.println("PEBBLE:\n" + pebbleDdl);
            System.out.println("NORM LEGACY:\n" + normLegacy);
            System.out.println("NORM PEBBLE:\n" + normPebble);
        }

        assertEquals(normLegacy, normPebble);
    }

    @Test
    void shouldFormatDdlWhenRequested() {
        // Given
        var model = createSimpleOrderModel();
        var generator = new PebblePostgresDdlGenerator();

        // When
        var unformattedDdl = generator.generateDdl(model);
        var formattedDdl = generator.generateFormattedDdl(model);

        // Then
        // Formatted DDL should contain line breaks and proper indentation
        // The formatted version should be different from the unformatted version
        // (unless the unformatted version happens to already be perfectly formatted)

        // Both should be valid SQL
        assert unformattedDdl.contains("CREATE TABLE");
        assert formattedDdl.contains("CREATE TABLE");

        // Formatted version should have consistent formatting
        var reformatted = SqlFormatter.of(Dialect.PostgreSql).format(formattedDdl);
        assertEquals(formattedDdl, reformatted, "Formatted DDL should be idempotent");
    }

    private SddModel createSimpleOrderModel() {
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());

        // Entity attributes
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var customerIdAttr = new AttributeDef("customer_id", "int", false, false, null, "Customer reference");
        var totalAmountAttr = new AttributeDef("total_amount", "numeric(10,2)", false, false, null, null);

        // Initial state: pending
        var pendingState = new StateDef(
                "pending",
                "order_pending",
                true, // initial
                List.of(), // no from states (initial)
                List.of(), // no OR transitions
                Map.of("pending_reason",
                        new AttributeDef("pending_reason", "text", false, false, null, null)));

        var entity = new EntityDef(
                "order",
                "orders",
                idAttr,
                Map.of("customer_id", customerIdAttr, "total_amount", totalAmountAttr),
                Map.of("pending", pendingState),
                Map.of(), // no extensions
                Map.of()); // no projections

        return new SddModel("0.1", "simple-order-model", database, Map.of("order", entity));
    }
}
