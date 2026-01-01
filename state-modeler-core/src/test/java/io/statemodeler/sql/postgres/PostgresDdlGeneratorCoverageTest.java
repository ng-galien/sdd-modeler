package io.statemodeler.sql.postgres;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.core.*;
import io.statemodeler.sql.DdlGenerators;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Additional tests to exercise Postgres DDL generator behavior and coverage.
 * These tests cover edge cases and error paths not exercised by integration tests.
 */
class PostgresDdlGeneratorCoverageTest {

    @Test
    void shouldHandleCurrentStateViewWithoutIntervalsView() {
        // Given - Entity with only current_state projection (no intervals)
        var database = new DatabaseConfig("postgres", "test", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var initialState = new StateDef("active", "test_active", true, (String) null, List.of(), Map.of());

        var entity = new EntityDef(
                "test",
                "test_table",
                idAttr,
                Map.of(),
                Map.of("active", initialState),
                Map.of(),
                Map.of(
                        "current",
                        new ProjectionDef(
                                "current", "test_current_state", ProjectionDef.ProjectionKind.CURRENT_STATE)));

        var model = new SddModel("1.0.0", "test", database, Map.of("test", entity));
        var generator = DdlGenerators.forDialect("postgres");

        // When
        var ddl = generator.generateDdl(model);

        // Then - Should generate view with fallback intervals view name
        assertNotNull(ddl);
        assertTrue(ddl.contains("FROM test_states.test_state_intervals"), "Should use fallback intervals view name");
    }

    @Test
    void shouldHandleMultipleProjectionsSorting() {
        // Given - Entity with multiple projections including intervals and current_state
        var database = new DatabaseConfig("postgres", "test", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var initialState = new StateDef("active", "test_active", true, (String) null, List.of(), Map.of());

        var secondState = new StateDef("inactive", "test_inactive", false, "active", List.of(), Map.of());

        var entity = new EntityDef(
                "test",
                "test_table",
                idAttr,
                Map.of(),
                Map.of("active", initialState, "inactive", secondState),
                Map.of(),
                Map.of(
                        "current",
                                new ProjectionDef(
                                        "current", "test_current_state", ProjectionDef.ProjectionKind.CURRENT_STATE),
                        "intervals",
                                new ProjectionDef(
                                        "intervals", "test_state_intervals", ProjectionDef.ProjectionKind.INTERVALS)));

        var model = new SddModel("1.0.0", "test", database, Map.of("test", entity));
        var generator = DdlGenerators.forDialect("postgres");

        // When
        var ddl = generator.generateDdl(model);

        // Then - Intervals view should come before current_state view
        assertNotNull(ddl);
        var intervalsIndex = ddl.indexOf("CREATE VIEW test_states.test_state_intervals");
        var currentIndex = ddl.indexOf("CREATE VIEW test_states.test_current_state");
        assertTrue(intervalsIndex < currentIndex, "Intervals view should be created before current_state view");
    }

    @Test
    void shouldNotUseInlineColumnReferences() {
        // Verifies that all FK are now added as ALTER TABLE constraints

        var database = new DatabaseConfig("postgres", "test", null, java.util.Map.of());
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var refAttr = new AttributeDef("parent_id", "INTEGER", true, false, null, null);

        var initialState =
                new StateDef("active", "test_active", true, (String) null, List.of(), Map.of("data", refAttr));

        var entity = new EntityDef(
                "test", "test_table", idAttr, Map.of(), Map.of("active", initialState), Map.of(), Map.of());

        var model = new SddModel("1.0.0", "test", database, Map.of("test", entity));
        var generator = DdlGenerators.forDialect("postgres");

        // When
        var ddl = generator.generateDdl(model);

        // Then - All FK should be via ALTER TABLE, not inline
        assertNotNull(ddl);
        assertTrue(ddl.contains("ALTER TABLE"), "Should have ALTER TABLE for FK");

        // Check that state table creation doesn't have inline REFERENCES
        var createTableStart = ddl.indexOf("CREATE TABLE test_states.test_active");
        var nextAlterTable = ddl.indexOf("ALTER TABLE", createTableStart);
        var createTableSection = ddl.substring(createTableStart, nextAlterTable);

        assertFalse(createTableSection.contains("REFERENCES"), "CREATE TABLE should not contain inline REFERENCES");
    }
}
