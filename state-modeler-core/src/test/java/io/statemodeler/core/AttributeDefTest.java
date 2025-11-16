package io.statemodeler.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AttributeDefTest {

    @Test
    void shouldCreateValidAttributeDef() {
        // Given & When
        var attr = new AttributeDef("id", "serial", false, true, "DEFAULT 1", "Primary key");

        // Then
        assertThat(attr.name()).isEqualTo("id");
        assertThat(attr.type()).isEqualTo("serial");
        assertThat(attr.nullable()).isFalse();
        assertThat(attr.primaryKey()).isTrue();
        assertThat(attr.defaultValue()).isEqualTo("DEFAULT 1");
        assertThat(attr.description()).isEqualTo("Primary key");
    }

    @Test
    void shouldCreateAttributeDefWithNullOptionals() {
        // Given & When
        var attr = new AttributeDef("customer_id", "int", true, false, null, null);

        // Then
        assertThat(attr.name()).isEqualTo("customer_id");
        assertThat(attr.type()).isEqualTo("int");
        assertThat(attr.nullable()).isTrue();
        assertThat(attr.primaryKey()).isFalse();
        assertThat(attr.defaultValue()).isNull();
        assertThat(attr.description()).isNull();
    }

    @Test
    void shouldRejectNullName() {
        assertThatThrownBy(() -> new AttributeDef(null, "int", false, false, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null");
    }

    @Test
    void shouldRejectNullType() {
        assertThatThrownBy(() -> new AttributeDef("id", null, false, false, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type cannot be null");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var attr1 = new AttributeDef("id", "serial", false, true, "DEFAULT 1", "Primary key");
        var attr2 = new AttributeDef("id", "serial", false, true, "DEFAULT 1", "Primary key");
        var attr3 = new AttributeDef("other_id", "serial", false, true, "DEFAULT 1", "Primary key");

        // Then
        assertThat(attr1).isEqualTo(attr2);
        assertThat(attr1).isNotEqualTo(attr3);
        assertThat(attr1.hashCode()).isEqualTo(attr2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var attr = new AttributeDef("id", "serial", false, true, "DEFAULT 1", "Primary key");

        // When
        var result = attr.toString();

        // Then
        assertThat(result)
                .contains("AttributeDef")
                .contains("name=id")
                .contains("type=serial")
                .contains("nullable=false")
                .contains("primaryKey=true");
    }

    @Test
    void shouldHandleEdgeCases() {
        // Empty string values (allowed)
        var attrEmptyDefaults = new AttributeDef("test", "varchar", true, false, "", "");

        assertThat(attrEmptyDefaults.defaultValue()).isEmpty();
        assertThat(attrEmptyDefaults.description()).isEmpty();

        // Different boolean combinations
        var attrNullablePrimary = new AttributeDef("weird", "int", true, true, null, null);
        assertThat(attrNullablePrimary.nullable()).isTrue();
        assertThat(attrNullablePrimary.primaryKey()).isTrue();
    }

    @Test
    void shouldCreateCommonDatabaseTypes() {
        // Test various common database types
        var stringAttr = new AttributeDef("name", "varchar(255)", true, false, null, "User name");
        var intAttr = new AttributeDef("count", "integer", false, false, "0", null);
        var decimalAttr = new AttributeDef("price", "decimal(10,2)", false, false, "0.00", "Price in euros");
        var timestampAttr = new AttributeDef("created_at", "timestamptz", false, false, "NOW()", null);

        assertThat(stringAttr.type()).isEqualTo("varchar(255)");
        assertThat(intAttr.type()).isEqualTo("integer");
        assertThat(decimalAttr.type()).isEqualTo("decimal(10,2)");
        assertThat(timestampAttr.type()).isEqualTo("timestamptz");
    }
}
