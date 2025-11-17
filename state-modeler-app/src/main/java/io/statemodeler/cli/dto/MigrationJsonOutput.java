package io.statemodeler.cli.dto;

/**
 * DTO for serialized migration JSON output.
 */
public record MigrationJsonOutput(double confidence, String comments, String migrationScript) {}
