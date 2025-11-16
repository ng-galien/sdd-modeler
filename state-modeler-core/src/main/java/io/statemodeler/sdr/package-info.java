/**
 * State Definition Record (SDR) - Immutable snapshots with stable hashing.
 *
 * <p>This package provides the core SDR functionality:
 * <ul>
 *   <li>{@link io.statemodeler.sdr.SdrRecord} - Immutable record with schema, DDL, and hashes</li>
 *   <li>{@link io.statemodeler.sdr.SdrFactory} - SPI for creating SDRs</li>
 *   <li>{@link io.statemodeler.sdr.DefaultSdrFactory} - Default implementation with canonical JSON</li>
 *   <li>{@link io.statemodeler.sdr.SdrHasher} - SHA-256 utility for deterministic hashing</li>
 * </ul>
 *
 * <p>All types are non-null unless explicitly annotated with {@code @Nullable}.
 */
@NullMarked
package io.statemodeler.sdr;

import org.jspecify.annotations.NullMarked;
