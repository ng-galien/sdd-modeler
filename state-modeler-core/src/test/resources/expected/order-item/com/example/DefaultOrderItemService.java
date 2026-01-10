package com.example;

import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.Instant;

public class DefaultOrderItemService implements OrderItemService {

  private final CreatedRepository createdRepository;
  private final PendingPaymentRepository pendingPaymentRepository;
  private final OrderItemDomainStateRepository domainStateRepository;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  public DefaultOrderItemService(
      CreatedRepository createdRepository,
      PendingPaymentRepository pendingPaymentRepository,
      OrderItemDomainStateRepository domainStateRepository,
      com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
    this.createdRepository = createdRepository;
    this.pendingPaymentRepository = pendingPaymentRepository;
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
              case "PENDING_PAYMENT" -> OrderItemState.PendingPayment.class;
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

  @Override
  @Transactional
  public OrderItemDto transitionToPendingPayment(
      OrderItemId id, TransitionToPendingPaymentCommand command) {
    // Simple transition: capture previous state id directly
    Integer previousStateId = null;
    Optional<OrderItemState.Created> source0 = createdRepository.findById(id);
    if (source0.isPresent()) {
      previousStateId = source0.get().id();
      createdRepository.delete(source0.get());
    }
    if (previousStateId == null) {
      throw new IllegalStateException(
          "Entity " + id + " is not in a valid state to transition to pending_payment");
    }

    // Create new state with reference to previous state
    var newState = new OrderItemState.PendingPayment(
        null, id, previousStateId, command.paidAmount(), command.paymentMethod());
    pendingPaymentRepository.save(newState);

    // Return DTO (entity attributes are not available from state record)
    return new OrderItemDto(id, null);
  }
}
