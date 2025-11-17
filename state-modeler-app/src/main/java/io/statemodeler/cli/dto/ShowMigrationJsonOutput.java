package io.statemodeler.cli.dto;

/**
 * DTO for show-migration consolidated JSON output.
 */
public record ShowMigrationJsonOutput(
        SdrSummary original, SdrSummary newSdr, String diff, MigrationJsonOutput migration) {}
