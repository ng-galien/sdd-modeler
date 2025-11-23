package com.example;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record OrderItemId(UUID value) {

  @JsonCreator
  public static OrderItemId fromString(String value) {
    return new OrderItemId(UUID.fromString(value));
  }

  @JsonValue
  public String asString() {
    return value.toString();
  }
}
