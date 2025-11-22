package io.statemodeler.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class TriggerDefinitionTest {

    @Test
    void shouldCreateTriggerDefinition() {
        var trigger = new TriggerDefinition(
                "trig", "tbl", "AFTER", List.of("INSERT", "UPDATE"), "ROW", "func", List.of("arg"));

        assertThat(trigger.name()).isEqualTo("trig");
        assertThat(trigger.table()).isEqualTo("tbl");
        assertThat(trigger.timing()).isEqualTo("AFTER");
        assertThat(trigger.events()).containsExactly("INSERT", "UPDATE");
        assertThat(trigger.forEach()).isEqualTo("ROW");
        assertThat(trigger.functionName()).isEqualTo("func");
        assertThat(trigger.functionArgs()).containsExactly("arg");
    }

    @Test
    void shouldValidateConstructorArguments() {
        assertThatThrownBy(() -> new TriggerDefinition(null, "t", "AFTER", List.of("INSERT"), "ROW", "f", List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name cannot be null");

        assertThatThrownBy(() -> new TriggerDefinition("n", null, "AFTER", List.of("INSERT"), "ROW", "f", List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("table cannot be null");

        assertThatThrownBy(() -> new TriggerDefinition("n", "t", "INVALID", List.of("INSERT"), "ROW", "f", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timing must be 'BEFORE' or 'AFTER'");

        assertThatThrownBy(() -> new TriggerDefinition("n", "t", "AFTER", List.of(), "ROW", "f", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("events cannot be empty");

        assertThatThrownBy(() -> new TriggerDefinition("n", "t", "AFTER", List.of("INVALID"), "ROW", "f", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event must be 'INSERT', 'UPDATE', or 'DELETE'");

        assertThatThrownBy(() -> new TriggerDefinition("n", "t", "AFTER", List.of("INSERT"), "INVALID", "f", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("forEach must be 'ROW' or 'STATEMENT'");

        assertThatThrownBy(() -> new TriggerDefinition("n", "t", "AFTER", List.of("INSERT"), "ROW", null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("functionName cannot be null");

        assertThatThrownBy(() -> new TriggerDefinition("n", "t", "AFTER", List.of("INSERT"), "ROW", "f", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("functionArgs cannot be null");
    }
}
