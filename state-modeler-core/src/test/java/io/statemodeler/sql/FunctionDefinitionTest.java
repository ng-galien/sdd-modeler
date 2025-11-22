package io.statemodeler.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class FunctionDefinitionTest {

    @Test
    void shouldCreateFunctionDefinition() {
        var func = new FunctionDefinition(
                "my_func", "public", List.of("arg1 text"), "void", "plpgsql", "BEGIN END;");

        assertThat(func.name()).isEqualTo("my_func");
        assertThat(func.schema()).isEqualTo("public");
        assertThat(func.parameters()).containsExactly("arg1 text");
        assertThat(func.returnType()).isEqualTo("void");
        assertThat(func.language()).isEqualTo("plpgsql");
        assertThat(func.body()).isEqualTo("BEGIN END;");
        assertThat(func.fullName()).isEqualTo("public.my_func");
    }

    @Test
    void shouldValidateConstructorArguments() {
        assertThatThrownBy(() -> new FunctionDefinition(null, "s", List.of(), "v", "l", "b"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name cannot be null");

        assertThatThrownBy(() -> new FunctionDefinition("", "s", List.of(), "v", "l", "b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be blank");

        assertThatThrownBy(() -> new FunctionDefinition("n", null, List.of(), "v", "l", "b"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("schema cannot be null");

        assertThatThrownBy(() -> new FunctionDefinition("n", "s", null, "v", "l", "b"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("parameters cannot be null");

        assertThatThrownBy(() -> new FunctionDefinition("n", "s", List.of(), null, "l", "b"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("returnType cannot be null");

        assertThatThrownBy(() -> new FunctionDefinition("n", "s", List.of(), "v", null, "b"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("language cannot be null");

        assertThatThrownBy(() -> new FunctionDefinition("n", "s", List.of(), "v", "l", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("body cannot be null");
    }
}
