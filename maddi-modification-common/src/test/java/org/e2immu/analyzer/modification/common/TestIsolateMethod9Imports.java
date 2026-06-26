package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// real-world reproduction (OrderHelper.validateOrder): external types referenced by simple name across packages
// must be nested directly in the frame, not pushed into namespace stubs (which would break simple-name resolution)
public class TestIsolateMethod9Imports extends CommonIsolateMethodTest {

    @Language("java")
    public static final String ORDER_SERVICE = """
            package com.example.legacy.service;
            public interface IOrderService {
                String getOrderDetails(String orderId);
            }
            """;

    @Language("java")
    public static final String NOTIFICATION_SERVICE = """
            package com.example.legacy.service;
            public interface INotificationService {
                void log(String message);
            }
            """;

    @Language("java")
    public static final String COMPONENT_FACTORY = """
            package com.example.legacy.factory;
            import com.example.legacy.service.INotificationService;
            import com.example.legacy.service.IOrderService;
            public class ComponentFactory {
                public static INotificationService getNotificationService() { return null; }
                public static IOrderService getOrderService() { return null; }
            }
            """;

    @Language("java")
    public static final String ORDER_HELPER = """
            package com.example.legacy.helper;
            import com.example.legacy.factory.ComponentFactory;
            import com.example.legacy.service.INotificationService;
            import com.example.legacy.service.IOrderService;
            public class OrderHelper {
                private IOrderService getOrderService() {
                    return ComponentFactory.getOrderService();
                }
                public boolean validateOrder(String orderId) {
                    IOrderService orderService = getOrderService();
                    String details = orderService.getOrderDetails(orderId);
                    ComponentFactory.getNotificationService().log("Validating order: " + orderId);
                    return details != null && !details.isEmpty();
                }
            }
            """;

    // detailed sources on: this is how the real harness parses, and what lets IsolateMethod tell a simple-name
    // reference (-> frame) from a package-qualified one (-> namespace stub)
    private TypeInfo parseAll() {
        return javaInspector.parse(Map.of(
                "com.example.legacy.service.IOrderService", ORDER_SERVICE,
                "com.example.legacy.service.INotificationService", NOTIFICATION_SERVICE,
                "com.example.legacy.factory.ComponentFactory", COMPONENT_FACTORY,
                "com.example.legacy.helper.OrderHelper", ORDER_HELPER
        ), new JavaInspector.ParseOptions.Builder().setDetailedSources(true).build())
                .parseResult().findType("com.example.legacy.helper.OrderHelper");
    }

    @DisplayName("isolate validateOrder: imported types from several packages nest directly in the frame")
    @Test
    public void validateOrder() {
        TypeInfo orderHelper = parseAll();
        String m = """
                public boolean validateOrder(String orderId) {
                    IOrderService orderService = getOrderService();
                    String details = orderService.getOrderDetails(orderId);
                    ComponentFactory.getNotificationService().log("Validating order: " + orderId);
                    return details != null && !details.isEmpty();
                }""";
        String out = isolate(orderHelper, "validateOrder", 1, m);
        @Language("java")
        String expected = """
                public class OrderHelper_validateOrder {
                    class ComponentFactory {static INotificationService getNotificationService() { return null; } }
                    class INotificationService {void log(String message) { } }
                    class IOrderService {String getOrderDetails(String orderId) { return null; } }
                    IOrderService getOrderService() { return null; }
                    public boolean validateOrder(String orderId) {
                    IOrderService orderService = getOrderService();
                    String details = orderService.getOrderDetails(orderId);
                    ComponentFactory.getNotificationService().log("Validating order: " + orderId);
                    return details != null && !details.isEmpty();
                }
                }
                """;
        assertEquals(expected, out);
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("OrderHelper_validateOrder", out));
    }
}
