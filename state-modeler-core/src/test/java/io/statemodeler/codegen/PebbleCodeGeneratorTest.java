package io.statemodeler.codegen;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.core.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PebbleCodeGeneratorTest {

        @Test
        void shouldGenerateJavaFromModel() {
                var idAttr = new AttributeDef("id", "serial", false, true, null, null);
                var paidAmountAttr = new AttributeDef("paid_amount", "numeric(10,2)", false, false, null, null);
                var paymentMethodAttr = new AttributeDef("payment_method", "text", false, false, null, null);
                var createdAtAttr = new AttributeDef("created_at", "timestamptz", false, false, null, null);
                var created = new StateDef(
                                "created",
                                "order_created",
                                true,
                                java.util.List.of(),
                                java.util.List.of(),
                                Map.of());
                var pending = new StateDef(
                                "pending_payment",
                                "order_pending",
                                false,
                                java.util.List.of("created"),
                                java.util.List.of(),
                                Map.of("paid_amount", paidAmountAttr, "payment_method", paymentMethodAttr));
                var states = Map.of("pending_payment", pending, "created", created);
                var entity = new EntityDef("order_item", "orders", idAttr, Map.of("created_at", createdAtAttr), states,
                                Map.of(), Map.of());
                var database = new DatabaseConfig("postgres", "public", null, Map.of("packageName", "com.example"));
                var model = new SddModel("1.0", "test", database, Map.of(entity.name(), entity));

                CodeGenerator generator = CodeGenerators.forLanguage("java");
                var generated = generator.generate(model);

                assertNotNull(generated);
                assertTrue(generated.size() >= 1);
                var filename = "com/example/OrderItemState.java";
                assertTrue(generated.containsKey(filename), "Expected generated file: " + filename);
                var content = generated.get(filename);
                assertTrue(content.contains("interface OrderItemState"));
                assertTrue(content.contains("record PendingPayment("));
                assertTrue(content.contains("BigDecimal paidAmount"));
                assertTrue(content.contains("String paymentMethod"));

                var repoFilename = "com/example/PendingPaymentRepository.java";
                assertTrue(generated.containsKey(repoFilename), "Expected generated repository: " + repoFilename);
                var repoContent = generated.get(repoFilename);
                assertTrue(
                                repoContent.contains(
                                                "interface PendingPaymentRepository extends CrudRepository<OrderItemState.PendingPayment, OrderItemId>"));

                // Verify ID generation
                var idFilename = "com/example/OrderItemId.java";
                assertTrue(generated.containsKey(idFilename), "Expected generated ID: " + idFilename);
                assertTrue(generated.get(idFilename).contains("public record OrderItemId(UUID value)"));

                // Verify Converters generation
                var convertersFilename = "com/example/OrderItemConverters.java";
                assertTrue(generated.containsKey(convertersFilename),
                                "Expected generated Converters: " + convertersFilename);
                assertTrue(generated.get(convertersFilename).contains("class UuidToOrderItemIdConverter"));
                assertTrue(generated.get(convertersFilename).contains("class OrderItemIdToUuidConverter"));

                // Verify Config generation
                var configFilename = "com/example/SddConfig.java";
                assertTrue(generated.containsKey(configFilename), "Expected generated Config: " + configFilename);
                assertTrue(generated.get(configFilename)
                                .contains("new OrderItemConverters.UuidToOrderItemIdConverter()"));

                // Verify Entity annotations
                assertTrue(content.contains("@Table(\"orders\")"));
                assertTrue(content.contains("@Id OrderItemId id"));

                // Verify DTO generation
                var dtoFilename = "com/example/OrderItemDto.java";
                assertTrue(generated.containsKey(dtoFilename), "Expected generated DTO: " + dtoFilename);
                var dtoContent = generated.get(dtoFilename);
                assertTrue(dtoContent.contains("public record OrderItemDto("));
                assertTrue(dtoContent.contains("OrderItemId id"));
                assertTrue(dtoContent.contains("Instant createdAt"));

                // Verify Controller generation
                var controllerFilename = "com/example/OrderItemController.java";
                assertTrue(generated.containsKey(controllerFilename),
                                "Expected generated Controller: " + controllerFilename);
                var controllerContent = generated.get(controllerFilename);
                assertTrue(controllerContent.contains("@RestController"));
                assertTrue(controllerContent.contains("@RequestMapping(\"/api/order_items\")"));
                assertTrue(controllerContent.contains("public class OrderItemController"));
                assertTrue(controllerContent
                                .contains("public ResponseEntity<OrderItemDto> get(@PathVariable OrderItemId id)"));

                // Verify Service generation
                var serviceFilename = "com/example/OrderItemService.java";
                assertTrue(generated.containsKey(serviceFilename), "Expected generated Service: " + serviceFilename);
                var serviceContent = generated.get(serviceFilename);
                assertTrue(serviceContent.contains("@Service"));
                assertTrue(serviceContent.contains("public class OrderItemService"));
                assertTrue(serviceContent.contains("private final PendingPaymentRepository pendingPaymentRepository"));
                assertTrue(serviceContent.contains("private final CreatedRepository createdRepository"));
                // Check transition method and command
                assertTrue(serviceContent.contains("public record TransitionToPendingPaymentCommand"));
                assertTrue(serviceContent.contains(
                                "java.util.Objects.requireNonNull(paidAmount, \"paidAmount cannot be null\")"));
                assertTrue(serviceContent.contains(
                                "public OrderItemDto transitionToPendingPayment(OrderItemId id, TransitionToPendingPaymentCommand command)"));
                assertTrue(serviceContent.contains("createdRepository.findById(id)"));
                assertTrue(serviceContent.contains("createdRepository.delete(source0.get())"));
                assertTrue(serviceContent.contains("pendingPaymentRepository.save(newState)"));
        }
}
