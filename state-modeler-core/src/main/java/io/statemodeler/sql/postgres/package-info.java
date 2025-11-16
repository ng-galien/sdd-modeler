/**
 * PostgreSQL-specific DDL generation with null-safe-by-default semantics.
 * All types are non-null unless explicitly annotated with @Nullable.
 */
@NullMarked
package io.statemodeler.sql.postgres;

import org.jspecify.annotations.NullMarked;
