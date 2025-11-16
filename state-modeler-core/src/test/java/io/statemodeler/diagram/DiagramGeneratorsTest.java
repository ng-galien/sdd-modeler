package io.statemodeler.diagram;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.diagram.mermaid.MermaidDiagramGenerator;
import org.junit.jupiter.api.Test;

class DiagramGeneratorsTest {

    @Test
    void shouldReturnMermaidGeneratorForMermaidFormat() {
        // When
        var generator = DiagramGenerators.forFormat("mermaid");

        // Then
        assertInstanceOf(MermaidDiagramGenerator.class, generator);
        assertEquals("mermaid", generator.getFormat());
    }

    @Test
    void shouldBeCaseInsensitiveForFormat() {
        // When
        var generator1 = DiagramGenerators.forFormat("MERMAID");
        var generator2 = DiagramGenerators.forFormat("Mermaid");

        // Then
        assertInstanceOf(MermaidDiagramGenerator.class, generator1);
        assertInstanceOf(MermaidDiagramGenerator.class, generator2);
    }

    @Test
    void shouldThrowExceptionForUnsupportedFormat() {
        // When/Then
        var ex = assertThrows(IllegalArgumentException.class, () -> DiagramGenerators.forFormat("plantuml"));
        assertTrue(ex.getMessage().contains("Unsupported diagram format: plantuml"));
        assertTrue(ex.getMessage().contains("Supported formats: mermaid"));
    }

    @Test
    void shouldThrowExceptionForNullFormat() {
        // When/Then
        var ex = assertThrows(IllegalArgumentException.class, () -> DiagramGenerators.forFormat(null));
        assertTrue(ex.getMessage().contains("format cannot be null"));
    }

    @Test
    void shouldReturnTrueForSupportedFormat() {
        // When
        var isSupported = DiagramGenerators.isSupported("mermaid");

        // Then
        assertTrue(isSupported);
    }

    @Test
    void shouldReturnFalseForUnsupportedFormat() {
        // When
        var isSupported = DiagramGenerators.isSupported("plantuml");

        // Then
        assertFalse(isSupported);
    }

    @Test
    void shouldBeCaseInsensitiveForIsSupported() {
        // When/Then
        assertTrue(DiagramGenerators.isSupported("MERMAID"));
        assertTrue(DiagramGenerators.isSupported("Mermaid"));
        assertTrue(DiagramGenerators.isSupported("mermaid"));
    }

    @Test
    void shouldReturnFalseForNullInIsSupported() {
        // When
        var isSupported = DiagramGenerators.isSupported(null);

        // Then
        assertFalse(isSupported);
    }

    @Test
    void shouldReturnListOfSupportedFormats() {
        // When
        var formats = DiagramGenerators.getSupportedFormats();

        // Then
        assertArrayEquals(new String[] {"mermaid"}, formats);
    }
}
