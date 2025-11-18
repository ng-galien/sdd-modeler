package io.statemodeler.cli.dto;

/**
 * DTO representing a lightweight SDR summary with hash and DDL.
 */
public record SdrSummaryDto(String hash, String ddl) {}
