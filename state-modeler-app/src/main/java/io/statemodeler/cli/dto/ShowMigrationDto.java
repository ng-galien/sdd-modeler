package io.statemodeler.cli.dto;

/**
 * DTO for show-migration consolidated JSON output.
 */
public record ShowMigrationDto(SdrSummaryDto original, SdrSummaryDto newSdr, String diff, MigrationDto migration) {}
