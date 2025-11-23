package com.example;

import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import java.sql.SQLException;
import java.util.ArrayList;

@Configuration
public class SddConfig {

  @Bean
  public JdbcCustomConversions sddCustomConversions() {
    var converters = new ArrayList<>();
    converters.add(new OrderItemConverters.UuidToOrderItemIdConverter());
    converters.add(new OrderItemConverters.OrderItemIdToUuidConverter());
    converters.add(new JsonbToStringReadingConverter());
    converters.add(new StringToJsonbWritingConverter());
    return new JdbcCustomConversions(converters);
  }

  // Converters for web/path bindings (String -> Id and Id -> String)
  static class StringToOrderItemIdConverter implements Converter<String, OrderItemId> {
    @Override
    public OrderItemId convert(String source) {
      return source == null ? null : OrderItemId.fromString(source);
    }
  }

  static class OrderItemIdToStringConverter implements Converter<OrderItemId, String> {
    @Override
    public String convert(OrderItemId source) {
      return source == null ? null : source.asString();
    }
  }

  @Bean
  public Converter<String, OrderItemId> stringToOrderItemIdConverter() {
    return new StringToOrderItemIdConverter();
  }

  @Bean
  public Converter<OrderItemId, String> orderitemIdToStringConverter() {
    return new OrderItemIdToStringConverter();
  }

  @ReadingConverter
  static class JsonbToStringReadingConverter implements Converter<PGobject, String> {
    @Override
    public String convert(PGobject source) {
      return source == null ? null : source.getValue();
    }
  }

  @WritingConverter
  static class StringToJsonbWritingConverter implements Converter<String, PGobject> {
    @Override
    public PGobject convert(String source) {
      if (source == null) return null;
      PGobject pg = new PGobject();
      pg.setType("jsonb");
      try {
        pg.setValue(source);
      } catch (SQLException e) {
        throw new IllegalArgumentException("Invalid JSON", e);
      }
      return pg;
    }
  }
}
