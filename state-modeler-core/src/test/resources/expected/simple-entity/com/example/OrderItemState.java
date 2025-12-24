package com.example;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generated ADT for order_item states.
 */
public sealed interface OrderItemState {

  /**
   * State: created     */
  @Table("order_created")
  record Created(
      @Column("id") Integer id,

      @Id @Column("order_item_id") @JsonProperty("order_item_id")
      OrderItemId orderItemId)
      implements OrderItemState {
    public Created {
      java.util.Objects.requireNonNull(orderItemId, "orderItemId must not be null");
    }
  }
}
