package com.example;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import java.util.UUID;

public class OrderItemConverters {

  @ReadingConverter
  public static class UuidToOrderItemIdConverter implements Converter<UUID, OrderItemId> {
    @Override
    public OrderItemId convert(UUID source) {
      return new OrderItemId(source);
    }
  }

  @WritingConverter
  public static class OrderItemIdToUuidConverter implements Converter<OrderItemId, UUID> {
    @Override
    public UUID convert(OrderItemId source) {
      return source.value();
    }
  }
}
