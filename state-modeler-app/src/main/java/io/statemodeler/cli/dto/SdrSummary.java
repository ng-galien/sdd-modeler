package io.statemodeler.cli.dto;

/**
 * DTO representing a lightweight SDR summary with hash and DDL.
 */
public record SdrSummary(String hash, String ddl) {}
