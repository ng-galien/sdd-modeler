# SDD SQL Mapping

## Mapping Rules
- Keep a stable entity table for identity and non-state attributes.
- Create one append-only table per state, with required state attributes.
- Encode transitions as foreign keys to previous state rows.
- For OR transitions, use a mapping table that references allowed predecessors.
- Externalize optional, non-decisional data into 1:1 extension tables.
- Derive the current state through views that compute state intervals and select the open interval.
- Favor a single timestamp per state row and derive end times from the next state start to avoid
  mutable end columns.

## Example Schema (Simplified)
```sql
-- Entity table (stable data)
CREATE TABLE orders (
  id SERIAL PRIMARY KEY,
  customer_id INTEGER NOT NULL,
  total_amount NUMERIC(10,2) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- State tables (append-only facts)
CREATE TABLE order_pending (
  id SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL REFERENCES orders(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  pending_reason TEXT NOT NULL
);

CREATE TABLE order_paid (
  id SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL REFERENCES orders(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  previous_pending_id INTEGER NOT NULL REFERENCES order_pending(id),
  payment_method TEXT NOT NULL,
  paid_amount NUMERIC(10,2) NOT NULL
);

CREATE TABLE cancelled_source (
  id SERIAL PRIMARY KEY,
  previous_pending_id INTEGER REFERENCES order_pending(id),
  previous_paid_id INTEGER REFERENCES order_paid(id),
  CHECK (
    (previous_pending_id IS NOT NULL AND previous_paid_id IS NULL) OR
    (previous_pending_id IS NULL AND previous_paid_id IS NOT NULL)
  )
);

CREATE TABLE order_cancelled (
  id SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL REFERENCES orders(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  previous_source_id INTEGER NOT NULL REFERENCES cancelled_source(id),
  cancel_reason TEXT NOT NULL
);

CREATE TABLE order_refunded (
  id SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL REFERENCES orders(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  previous_paid_id INTEGER NOT NULL REFERENCES order_paid(id),
  refund_amount NUMERIC(10,2) NOT NULL,
  refund_method TEXT NOT NULL
);

-- Example extension table (optional, non-decisional data)
CREATE TABLE order_paid_extensions (
  paid_id INTEGER PRIMARY KEY REFERENCES order_paid(id),
  additional_notes TEXT,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- View for current state (simplified; use intervals in practice)
CREATE VIEW current_order_states AS
SELECT order_id, 'PENDING' AS state_type, created_at
FROM order_pending
WHERE id NOT IN (SELECT previous_pending_id FROM order_paid)
UNION ALL
SELECT order_id, 'PAID' AS state_type, created_at
FROM order_paid
WHERE id NOT IN (SELECT previous_paid_id FROM order_refunded)
  AND id NOT IN (SELECT previous_paid_id FROM cancelled_source);
```

The example is intentionally minimal. For full temporal accuracy, use an intervals view that derives
end timestamps from the next state start, then filter `end_at IS NULL` to compute the current state.
