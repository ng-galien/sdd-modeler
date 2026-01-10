package com.example;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
public class OrderItemController implements OrderItemApi {

  // State-based entity - inject service and all state repositories
  private final OrderItemService service;
  private final PendingPaymentRepository pendingPaymentRepository;
  private final CreatedRepository createdRepository;

  public OrderItemController(
      OrderItemService service,
      PendingPaymentRepository pendingPaymentRepository,
      CreatedRepository createdRepository) {
    this.service = service;
    this.pendingPaymentRepository = pendingPaymentRepository;
    this.createdRepository = createdRepository;
  }

  // GET endpoints per state
  @Override
  public java.util.List<OrderItemService.OrderItemStateInfo> findAll() {
    return service.findAll();
  }

  @Override
  public ResponseEntity<OrderItemDto> getPendingPayment(OrderItemId id) {
    return pendingPaymentRepository
        .findById(id)
        .map(entity -> ResponseEntity.ok(new OrderItemDto(entity.orderItemId(), null)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<OrderItemDto> getCreated(OrderItemId id) {
    return createdRepository
        .findById(id)
        .map(entity -> ResponseEntity.ok(new OrderItemDto(entity.orderItemId(), null)))
        .orElse(ResponseEntity.notFound().build());
  }

  // POST endpoints for transitions
  @Override
  public ResponseEntity<OrderItemDto> transitionToPendingPayment(
      OrderItemId id, OrderItemService.TransitionToPendingPaymentCommand command) {
    OrderItemDto dto = service.transitionToPendingPayment(id, command);
    return ResponseEntity.ok(dto);
  }
}
