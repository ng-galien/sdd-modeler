package com.example;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Instant;

@Configuration
public class OrderItemServiceAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(OrderItemService.class)
  public OrderItemService orderItemService(
      CreatedRepository createdRepository,
      OrderItemDomainStateRepository domainStateRepository,
      com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
    return new DefaultOrderItemService(createdRepository, domainStateRepository, objectMapper);
  }
}
