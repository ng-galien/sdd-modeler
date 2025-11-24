package com.example;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderItemDto(OrderItemId id, Instant createdAt) {}
