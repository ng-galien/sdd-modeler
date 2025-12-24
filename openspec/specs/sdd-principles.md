# State-Driven Design (SDD) Principles

## Core Modeling Principles
- Model business states as an algebraic data type (ADT): a closed, exclusive set of variants with
  state-specific attributes.
- Keep one domain facet per ADT (mono-domain). If a concept mixes facets (payment vs shipping),
  split into parallel ADTs.
- Be exhaustive at a given time: the model covers all valid states for that facet. Adding a state
  is a deliberate model version change.
- Enforce uniqueness: at any time, one entity has exactly one active state per facet.
- Derive state from data, not a magic status flag. The current state is the latest open state in
  the history.
- Make states temporal: every state is a time interval. Without time, it is a property, not a
  state.
- Use explicit transitions: each new state is created by a known transition, and terminal states
  have no outgoing transitions.

## Stability and Evolution
- States are closed, attributes are open: do not add new states casually, but allow states to gain
  new attributes over time.
- Optional data is non-decisional. If an attribute changes state membership, it is decisional and
  must be part of the state or a transition.
- Keep invariants local to each state. Constraints belong with the state that owns them.
- Preserve history: states are append-only facts. Create a new state to change behavior, do not
  update or delete past states.

## Anti-Patterns to Avoid
- Boolean flags that pretend to be states (e.g., is_paid, is_cancelled). They create ambiguous or
  impossible combinations and lose ordering.
- One big status enum with cross-domain meanings, which hides business rules in application logic.
- Mixing multiple facets in a single status/flag set, which allows illegal combinations.

## Example: Order Payment Facet
A single facet ADT for payment can be modeled as:
- States: Pending, Paid, Cancelled, Refunded
- Transitions:
  - Pending -> Paid
  - Pending -> Cancelled
  - Paid -> Cancelled
  - Paid -> Refunded
- State-specific fields:
  - Pending: pending_reason
  - Paid: payment_method, paid_amount
  - Cancelled: cancel_reason
  - Refunded: refund_amount, refund_method
- Optional notes do not change state membership, so they belong in an extension structure. If a
  field changes which state applies, it is decisional and must be part of the state or transition.
