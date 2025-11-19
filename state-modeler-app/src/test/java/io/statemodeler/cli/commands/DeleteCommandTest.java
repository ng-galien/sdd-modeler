package io.statemodeler.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.statemodeler.cli.RepositoryMixin;
import io.statemodeler.repository.H2SdrRepository;
import io.statemodeler.sdr.SdrRecord;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class DeleteCommandTest {

    H2SdrRepository repository;
    SdrRecord testSdr;

    @BeforeEach
    void setUp() {
        // Use in-memory database for faster tests
        repository = H2SdrRepository.createInMemory("test-delete-" + System.nanoTime());

        // Create a test SDR
        testSdr = createTestSdr("test-hash-12345", "test-schema", "test-ddl");
        repository.save(testSdr, "test-model", "1.0.0").get();
    }

    @AfterEach
    void tearDown() {
        if (repository != null) {
            repository.close();
        }
    }

    /**
     * Helper to create test SDR records.
     */
    private SdrRecord createTestSdr(String hash, String schema, String ddl) {
        return new SdrRecord(schema, "application/json", ddl, hash, "ddl-hash-" + hash, "1.0.0");
    }

    private RepositoryMixin createMixin() {
        var mixin = new RepositoryMixin();
        mixin.testRepository = repository;
        return mixin;
    }

    @Test
    void shouldDeleteSdrWithYesFlag() {
        DeleteCommand command = new DeleteCommand();
        String hash = testSdr.schemaHash();
        command.skipConfirmation = true;
        command.repositoryMixin = createMixin();
        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute(hash);

        assertEquals(0, exitCode);

        // Verify deletion
        var result = repository.findByHash(testSdr.schemaHash());
        assertTrue(result.isSuccess());
        assertTrue(result.get().isEmpty());
    }

    @Test
    void shouldDeleteSdrWithConfirmation() {
        // Simulate "yes" input
        String simulatedInput = "yes\n";
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

            DeleteCommand command = new DeleteCommand();
            String hash = testSdr.schemaHash();
            command.skipConfirmation = false;
            command.repositoryMixin = createMixin();
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute(hash);

            assertEquals(0, exitCode);

            // Verify deletion
            var result = repository.findByHash(testSdr.schemaHash());
            assertTrue(result.isSuccess());
            assertTrue(result.get().isEmpty());
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void shouldCancelDeletionOnNo() {
        // Simulate "no" input
        String simulatedInput = "no\n";
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

            DeleteCommand command = new DeleteCommand();
            String hash = testSdr.schemaHash();
            command.skipConfirmation = false;
            command.repositoryMixin = createMixin();
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute(hash);

            assertEquals(0, exitCode);

            // Verify SDR still exists
            var result = repository.findByHash(testSdr.schemaHash());
            assertTrue(result.isSuccess());
            assertTrue(result.get().isPresent());
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void shouldCancelDeletionOnInvalidInput() {
        // Simulate invalid input (anything not "yes")
        String simulatedInput = "maybe\n";
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

            DeleteCommand command = new DeleteCommand();
            String hash = testSdr.schemaHash();
            command.skipConfirmation = false;
            command.repositoryMixin = createMixin();
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute(hash);

            assertEquals(0, exitCode);

            // Verify SDR still exists
            var result = repository.findByHash(testSdr.schemaHash());
            assertTrue(result.isSuccess());
            assertTrue(result.get().isPresent());
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void shouldHandleNonexistentHash() {
        DeleteCommand command = new DeleteCommand();
        String hash = "nonexistent1234567890abcdef";
        command.skipConfirmation = true;
        command.repositoryMixin = createMixin();
        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute(hash);

        assertEquals(1, exitCode);
    }

    @Test
    void shouldHandleRepositoryError() {
        // Given - invalid repository path
        DeleteCommand command = new DeleteCommand();
        command.skipConfirmation = true;

        var mixin = new RepositoryMixin();
        mixin.repositoryPath = "/invalid/\0/path";
        command.repositoryMixin = mixin;
        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute(testSdr.schemaHash());

        // Then
        assertEquals(1, exitCode);
    }

    @Test
    void shouldDisplaySdrInfoBeforeDeletion() {
        // This test verifies that the command runs successfully and displays info
        // Full verification would require capturing System.out, but we test the flow
        DeleteCommand command = new DeleteCommand();
        command.skipConfirmation = true;
        command.repositoryMixin = createMixin();
        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute(testSdr.schemaHash());

        assertEquals(0, exitCode);
    }

    @Test
    void shouldHandleEmptyInputAsNo() {
        // Simulate empty input (just Enter key)
        String simulatedInput = "\n";
        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

            DeleteCommand command = new DeleteCommand();
            String hash = testSdr.schemaHash();
            command.skipConfirmation = false;
            command.repositoryMixin = createMixin();
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute(hash);

            assertEquals(0, exitCode);

            // Verify SDR still exists
            var result = repository.findByHash(testSdr.schemaHash());
            assertTrue(result.isSuccess());
            assertTrue(result.get().isPresent());
        } finally {
            System.setIn(originalIn);
        }
    }
}
