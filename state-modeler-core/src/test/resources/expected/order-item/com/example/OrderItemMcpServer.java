package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import java.util.List;
import java.util.UUID;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class OrderItemMcpServer {

  private final OrderItemService orderItemService;
  private final ObjectMapper objectMapper;

  public OrderItemMcpServer(OrderItemService orderItemService, ObjectMapper objectMapper) {
    this.orderItemService = orderItemService;
    this.objectMapper = objectMapper;
  }

  @McpTool(
      name = "orderItem-transition-to-pending_payment",
      description = "Transition OrderItem to pending_payment")
  public OrderItemDto transitionToPendingPayment(
      @McpToolParam(description = "OrderItem id", required = true) UUID id,
      @McpToolParam(description = "paid_amount", required = true) BigDecimal paidAmount,
      @McpToolParam(description = "payment_method", required = true) String paymentMethod) {
    var command = new OrderItemService.TransitionToPendingPaymentCommand(paidAmount, paymentMethod);
    return orderItemService.transitionToPendingPayment(new OrderItemId(id), command);
  }

  @McpResource(
      uri = "sdd://orderItem/states",
      name = "OrderItem States",
      description = "Current OrderItem states",
      mimeType = "application/json")
  public ReadResourceResult getOrderItemStatesResource() {
    var states = orderItemService.findAll();
    try {
      var payload = objectMapper.writeValueAsString(states);
      return new ReadResourceResult(
          List.of(new TextResourceContents("sdd://orderItem/states", "application/json", payload)));
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize OrderItem states", e);
    }
  }

  @McpPrompt(name = "orderItem-summary", description = "Summarize OrderItem states")
  public GetPromptResult orderItemSummaryPrompt() {
    var states = orderItemService.findAll();
    var message = "OrderItem states: " + states.size();
    return new GetPromptResult(
        "OrderItem Summary", List.of(new PromptMessage(Role.ASSISTANT, new TextContent(message))));
  }
}
