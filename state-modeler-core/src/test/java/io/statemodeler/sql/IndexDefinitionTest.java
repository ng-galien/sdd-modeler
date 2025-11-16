package io.statemodeler.sql;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class IndexDefinitionTest {

    @Test
    void shouldCreateValidIndexDefinition() {
        // Given & When
        var index = new IndexDefinition("idx_order_paid_order_id", "order_paid", "public", List.of("order_id"), false);

        // Then
        assertThat(index.name()).isEqualTo("idx_order_paid_order_id");
        assertThat(index.table()).isEqualTo("order_paid");
        assertThat(index.schema()).isEqualTo("public");
        assertThat(index.columns()).containsExactly("order_id");
        assertThat(index.unique()).isFalse();
    }

    @Test
    void shouldCreateUniqueIndex() {
        // Given & When
        var index = new IndexDefinition("idx_unique_email", "users", "public", List.of("email"), true);

        // Then
        assertThat(index.unique()).isTrue();
    }

    @Test
    void shouldCreateIndexWithNullSchema() {
        // Given & When
        var index = new IndexDefinition("idx_name", "table", null, List.of("col"), false);

        // Then
        assertThat(index.schema()).isNull();
        assertThat(index.fullTableName()).isEqualTo("table");
    }

    @Test
    void shouldCreateIndexWithEmptySchema() {
        // Given & When
        var index = new IndexDefinition("idx_name", "table", "", List.of("col"), false);

        // Then
        assertThat(index.schema()).isEqualTo("");
        assertThat(index.fullTableName()).isEqualTo("table");
    }

    @Test
    void shouldCreateSchemaQualifiedTableName() {
        // Given & When
        var index = new IndexDefinition("idx_name", "table", "myschema", List.of("col"), false);

        // Then
        assertThat(index.fullTableName()).isEqualTo("myschema.table");
    }

    @Test
    void shouldCreateMultiColumnIndex() {
        // Given & When
        var index = new IndexDefinition("idx_composite", "table", "public", List.of("col1", "col2", "col3"), false);

        // Then
        assertThat(index.columns()).containsExactly("col1", "col2", "col3");
    }

    @Test
    void shouldCreateImmutableColumns() {
        // Given
        var columns = List.of("col1", "col2");
        var index = new IndexDefinition("idx_name", "table", "public", columns, false);

        // When & Then - verify columns are immutable
        assertThatThrownBy(() -> index.columns().add("col3")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectNullName() {
        // When & Then
        assertThatThrownBy(() -> new IndexDefinition(null, "table", "public", List.of("col"), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null");
    }

    @Test
    void shouldRejectNullTable() {
        // When & Then
        assertThatThrownBy(() -> new IndexDefinition("idx_name", null, "public", List.of("col"), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("table cannot be null");
    }

    @Test
    void shouldRejectNullColumns() {
        // When & Then
        assertThatThrownBy(() -> new IndexDefinition("idx_name", "table", "public", null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("columns cannot be null");
    }

    @Test
    void shouldRejectEmptyColumns() {
        // When & Then
        assertThatThrownBy(() -> new IndexDefinition("idx_name", "table", "public", List.of(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("columns cannot be empty");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var index1 = new IndexDefinition("idx_name", "table", "public", List.of("col"), false);
        var index2 = new IndexDefinition("idx_name", "table", "public", List.of("col"), false);
        var index3 = new IndexDefinition("idx_other", "table", "public", List.of("col"), false);

        // Then
        assertThat(index1).isEqualTo(index2);
        assertThat(index1).hasSameHashCodeAs(index2);
        assertThat(index1).isNotEqualTo(index3);
    }

    @Test
    void shouldImplementToString() {
        // Given
        var index = new IndexDefinition("idx_name", "table", "public", List.of("col1", "col2"), true);

        // When
        var toString = index.toString();

        // Then
        assertThat(toString).contains("idx_name");
        assertThat(toString).contains("table");
        assertThat(toString).contains("public");
        assertThat(toString).contains("col1");
        assertThat(toString).contains("col2");
        assertThat(toString).contains("true");
    }
}
