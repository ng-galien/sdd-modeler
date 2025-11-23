package com.example;

import java.math.BigDecimal;
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
   * State: pending_payment     */
  @Table("order_pending")
  record PendingPayment(
      @Column("id") Integer id,

      @Id @Column("order_item_id") @JsonProperty("order_item_id")
      OrderItemId orderItemId,

      @Column("payment_method") @JsonProperty("payment_method")
      String paymentMethod,

      @Column("paid_amount") @JsonProperty("paid_amount") BigDecimal paidAmount)
      implements OrderItemState {}
  /**
   * State: created     */
  @Table("order_created")
  record Created(
      @Column("id") Integer id,

      @Id @Column("order_item_id") @JsonProperty("order_item_id")
      OrderItemId orderItemId)
      implements OrderItemState {}
}
