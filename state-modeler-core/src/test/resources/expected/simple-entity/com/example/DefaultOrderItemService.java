package com.example;

import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.time.Instant;

public class DefaultOrderItemService implements OrderItemService {

  private final CreatedRepository createdRepository;
  private final OrderItemDomainStateRepository domainStateRepository;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  public DefaultOrderItemService(
      CreatedRepository createdRepository,
      OrderItemDomainStateRepository domainStateRepository,
      com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
    this.createdRepository = createdRepository;
    this.domainStateRepository = domainStateRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public java.util.List<OrderItemStateInfo> findAll() {
    var domainStates = domainStateRepository.findAll();
    var states = new java.util.ArrayList<OrderItemStateInfo>();
    for (var ds : domainStates) {
      try {
        Class<? extends OrderItemState> stateClass =
            switch (ds.stateType()) {
              case "CREATED" -> OrderItemState.Created.class;
              default -> throw new IllegalStateException("Unknown state type: " + ds.stateType());
            };
        var state = objectMapper.readValue(ds.stateJson(), stateClass);
        states.add(new OrderItemStateInfo(ds.stateType(), state));
      } catch (Exception e) {
        throw new RuntimeException("Failed to deserialize state", e);
      }
    }
    return states;
  }
}
