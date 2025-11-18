package io.statemodeler.cli.dto;

/**
 * DTO for serialized migration JSON output.
 */
public record MigrationDto(double confidence, String comments, String migrationScript) {}
