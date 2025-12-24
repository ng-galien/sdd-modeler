package io.statemodeler.codegen;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.core.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GenerateServiceDebugTest {

    @Test
    void generateServiceAndPrint() {
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var paidAmountAttr = new AttributeDef("paid_amount", "numeric(10,2)", false, false, null, null);
        var paymentMethodAttr = new AttributeDef("payment_method", "text", false, false, null, null);
        var createdAtAttr = new AttributeDef("created_at", "timestamptz", false, false, null, null);
        var created =
                new StateDef("created", "order_created", true, java.util.List.of(), java.util.List.of(), Map.of());
        var pending = new StateDef(
                "pending_payment",
                "order_pending",
                false,
                java.util.List.of("created"),
                java.util.List.of(),
                Map.of("paid_amount", paidAmountAttr, "payment_method", paymentMethodAttr));
        var states = Map.of("pending_payment", pending, "created", created);
        var entity = new EntityDef(
                "order_item", "orders", idAttr, Map.of("created_at", createdAtAttr), states, Map.of(), Map.of());
        var database = new DatabaseConfig("postgres", "public", null, Map.of("packageName", "com.example"));
        var model = new SddModel("1.0.0", "test", database, Map.of(entity.name(), entity));

        CodeGenerator generator = CodeGenerators.forLanguage("java");
        var generated = generator.generate(model);
        var impl = generated.get("com/example/DefaultOrderItemService.java");
        System.out.println("DefaultOrderItemService.java content:\n" + impl);
        assertTrue(
                impl.contains("TransitionToPendingPaymentCommand"),
                "Expected generated service to contain transition command record");
        assertNotNull(impl);
    }
}
