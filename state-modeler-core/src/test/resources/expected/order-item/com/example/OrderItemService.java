package com.example;

import java.util.List;

public interface OrderItemService {

  // Returned record with state type and state payload for each entity
  record OrderItemStateInfo(String stateType, OrderItemState state) {}

  List<OrderItemStateInfo> findAll();

  OrderItemDto transitionToPendingPayment(
      OrderItemId id, TransitionToPendingPaymentCommand command);

  record TransitionToPendingPaymentCommand(BigDecimal paidAmount, String paymentMethod) {
    public TransitionToPendingPaymentCommand {
      java.util.Objects.requireNonNull(paidAmount, "paidAmount cannot be null");
      java.util.Objects.requireNonNull(paymentMethod, "paymentMethod cannot be null");
    }
  }
}
