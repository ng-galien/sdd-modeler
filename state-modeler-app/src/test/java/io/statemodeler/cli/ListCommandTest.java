package io.statemodeler.cli;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.repository.H2SdrRepository;
import io.statemodeler.sdr.DefaultSdrFactory;
import io.statemodeler.sdr.SdrFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ListCommand}.
 */
class ListCommandTest {

    private H2SdrRepository repository;
    private SdrFactory sdrFactory;

    @BeforeEach
    void setUp() {
        repository = H2SdrRepository.createInMemory("test-list-" + System.nanoTime());
        sdrFactory = new DefaultSdrFactory();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (repository != null) {
            repository.close();
        }
    }

    @Test
    void shouldListAllSdrsInTableFormat() {
        // Given - register some SDRs
        registerTestSdr("model1", "1.0");
        registerTestSdr("model2", "2.0");

        var command = new ListCommand();
        command.format = "table";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        // Capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("NAME"), "Should have table header");
        assertTrue(output.contains("VERSION"), "Should have VERSION column");
        assertTrue(output.contains("HASH"), "Should have HASH column");
        assertTrue(output.contains("model1"), "Should list model1");
        assertTrue(output.contains("model2"), "Should list model2");
        assertTrue(output.contains("Total: 2 SDR(s)"), "Should show total count");
    }

    @Test
    void shouldListSdrsInJsonFormat() {
        // Given
        registerTestSdr("json-model", "1.0");

        var command = new ListCommand();
        command.format = "json";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("\"sdrs\""), "Should have sdrs array");
        assertTrue(output.contains("\"name\""), "Should have name field");
        assertTrue(output.contains("\"json-model\""), "Should contain model name");
        assertTrue(output.contains("\"total\""), "Should have total field");
    }

    @Test
    void shouldListSdrsInYamlFormat() {
        // Given
        registerTestSdr("yaml-model", "3.0");

        var command = new ListCommand();
        command.format = "yaml";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("sdrs:"), "Should have sdrs key");
        assertTrue(output.contains("name:"), "Should have name field");
        assertTrue(output.contains("yaml-model"), "Should contain model name");
        assertTrue(output.contains("total:"), "Should have total field");
    }

    @Test
    void shouldHandleEmptyRepository() {
        // Given - empty repository
        var command = new ListCommand();
        command.format = "table";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(
                output.contains("No SDRs registered") || output.contains("Total: 0"),
                "Should indicate empty repository");
    }

    @Test
    void shouldRespectLimitOption() {
        // Given - register 5 SDRs
        for (int i = 1; i <= 5; i++) {
            registerTestSdr("model" + i, "1.0");
        }

        var command = new ListCommand();
        command.format = "table";
        command.limit = 3;
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("Total: 3 SDR(s)"), "Should limit to 3 results");
    }

    @Test
    void shouldRejectInvalidFormat() {
        // Given
        var command = new ListCommand();
        command.format = "xml"; // Invalid format
        command.limit = 0;
        command.repositoryMixin = createMixin();

        // When
        int exitCode = command.call();

        // Then
        assertEquals(1, exitCode, "Should fail with invalid format");
    }

    @Test
    void shouldHandleRepositoryError() {
        // Given - invalid repository path
        var command = new ListCommand();
        command.format = "table";
        command.limit = 0;

        var mixin = new RepositoryMixin();
        mixin.repositoryPath = "/invalid/\0/path";
        command.repositoryMixin = mixin;

        // When
        int exitCode = command.call();

        // Then
        assertEquals(1, exitCode, "Should fail with repository error");
    }

    @Test
    void shouldTruncateLongNames() {
        // Given - model with very long name
        String longName = "a".repeat(50);
        registerTestSdr(longName, "1.0");

        var command = new ListCommand();
        command.format = "table";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("..."), "Should truncate long names with ellipsis");
    }

    @Test
    void shouldEscapeSpecialCharactersInJson() {
        // Given - model with characters that need escaping in JSON
        registerTestSdr("model-with-newline", "1.0");

        var command = new ListCommand();
        command.format = "json";
        command.limit = 0;
        command.repositoryMixin = createMixin();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // When
        int exitCode = command.call();

        // Then
        System.setOut(originalOut);
        assertEquals(0, exitCode);

        String output = outContent.toString();
        assertTrue(output.contains("\"name\""), "Should have proper JSON structure");
        assertTrue(output.contains("model-with-newline"), "Should contain model name");
    }

    private void registerTestSdr(String modelName, String modelVersion) {
        String modelSource = """
                version: "0.1"
                name: "%s"
                database:
                  dialect: postgres
                entities:
                  order:
                    table: orders
                    id:
                      name: id
                      type: serial
                      primary_key: true
                    states:
                      pending:
                        initial: true
                        table: order_pending
                """.formatted(modelName);

        var sdr = sdrFactory.create(modelSource, "application/yaml", "postgres");

        try {
            repository.save(sdr, modelName, modelVersion);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register test SDR", e);
        }
    }

    private RepositoryMixin createMixin() {
        var mixin = new RepositoryMixin();
        mixin.testRepository = repository;
        return mixin;
    }
}
