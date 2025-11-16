package io.statemodeler.diagram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.statemodeler.diagram.mermaid.MermaidDiagramGenerator;
import org.junit.jupiter.api.Test;

class DiagramGeneratorsTest {

    @Test
    void shouldReturnMermaidGeneratorForMermaidFormat() {
        // When
        var generator = DiagramGenerators.forFormat("mermaid");

        // Then
        assertThat(generator).isInstanceOf(MermaidDiagramGenerator.class);
        assertThat(generator.getFormat()).isEqualTo("mermaid");
    }

    @Test
    void shouldBeCaseInsensitiveForFormat() {
        // When
        var generator1 = DiagramGenerators.forFormat("MERMAID");
        var generator2 = DiagramGenerators.forFormat("Mermaid");

        // Then
        assertThat(generator1).isInstanceOf(MermaidDiagramGenerator.class);
        assertThat(generator2).isInstanceOf(MermaidDiagramGenerator.class);
    }

    @Test
    void shouldThrowExceptionForUnsupportedFormat() {
        // When/Then
        assertThatThrownBy(() -> DiagramGenerators.forFormat("plantuml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported diagram format: plantuml")
                .hasMessageContaining("Supported formats: mermaid");
    }

    @Test
    void shouldThrowExceptionForNullFormat() {
        // When/Then
        assertThatThrownBy(() -> DiagramGenerators.forFormat(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("format cannot be null");
    }

    @Test
    void shouldReturnTrueForSupportedFormat() {
        // When
        var isSupported = DiagramGenerators.isSupported("mermaid");

        // Then
        assertThat(isSupported).isTrue();
    }

    @Test
    void shouldReturnFalseForUnsupportedFormat() {
        // When
        var isSupported = DiagramGenerators.isSupported("plantuml");

        // Then
        assertThat(isSupported).isFalse();
    }

    @Test
    void shouldBeCaseInsensitiveForIsSupported() {
        // When/Then
        assertThat(DiagramGenerators.isSupported("MERMAID")).isTrue();
        assertThat(DiagramGenerators.isSupported("Mermaid")).isTrue();
        assertThat(DiagramGenerators.isSupported("mermaid")).isTrue();
    }

    @Test
    void shouldReturnFalseForNullInIsSupported() {
        // When
        var isSupported = DiagramGenerators.isSupported(null);

        // Then
        assertThat(isSupported).isFalse();
    }

    @Test
    void shouldReturnListOfSupportedFormats() {
        // When
        var formats = DiagramGenerators.getSupportedFormats();

        // Then
        assertThat(formats).containsExactly("mermaid");
    }
}
