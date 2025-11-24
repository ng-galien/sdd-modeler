package com.example;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
public class OrderItemController implements OrderItemApi {

  // State-based entity - inject service and all state repositories
  private final OrderItemService service;
  private final CreatedRepository createdRepository;

  public OrderItemController(OrderItemService service, CreatedRepository createdRepository) {
    this.service = service;
    this.createdRepository = createdRepository;
  }

  // GET endpoints per state
  @Override
  public java.util.List<OrderItemService.OrderItemStateInfo> findAll() {
    return service.findAll();
  }

  @Override
  public ResponseEntity<OrderItemDto> getCreated(OrderItemId id) {
    return createdRepository
        .findById(id)
        .map(
            entity -> ResponseEntity.ok(new OrderItemDto(entity.orderItemId(), entity.createdAt())))
        .orElse(ResponseEntity.notFound().build());
  }

  // POST endpoints for transitions
}
