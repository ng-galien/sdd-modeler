package com.example;

import java.util.List;

import java.time.Instant;

public interface OrderItemService {

  // Returned record with state type and state payload for each entity
  record OrderItemStateInfo(String stateType, OrderItemState state) {}

  List<OrderItemStateInfo> findAll();
}
