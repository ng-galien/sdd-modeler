package com.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/order_items")
public interface OrderItemApi {

  @GetExchange
  java.util.List<OrderItemService.OrderItemStateInfo> findAll();

  @GetExchange("/created/{id}")
  ResponseEntity<OrderItemDto> getCreated(@PathVariable OrderItemId id);
}
