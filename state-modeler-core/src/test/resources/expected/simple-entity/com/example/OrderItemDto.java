package com.example;

import java.time.Instant;

public record OrderItemDto(OrderItemId id, Instant createdAt) {}
