package com.sky.service.ai.admin;

import com.sky.service.ai.AiToolCallingClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdminAiToolRegistry {

    public List<Map<String, Object>> adminTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(AiToolCallingClient.tool("get_shop_status", "Get the current shop opening status", Collections.emptyMap()));
        tools.add(AiToolCallingClient.tool("set_shop_status", "Open or close the shop after explicit confirmation",
                mapOf(
                        "status", AiToolCallingClient.stringProperty("Shop status: open or closed"),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "status", "confirmed"));
        tools.add(AiToolCallingClient.tool("query_orders", "Query admin orders or fetch order details",
                mapOf(
                        "order_id", integerProperty("Internal order ID for details"),
                        "order_number", AiToolCallingClient.stringProperty("Full or partial order number"),
                        "status", AiToolCallingClient.stringProperty("Order status: pending_payment, pending_accept, accepted, delivering, completed, cancelled"),
                        "page", integerProperty("Page number, default 1"),
                        "page_size", integerProperty("Page size for paged mode"),
                        "all", AiToolCallingClient.booleanProperty("Whether to fetch all matched records")
                )));
        tools.add(AiToolCallingClient.tool("get_order_statistics", "Get pending/dispatching order count statistics", Collections.emptyMap()));
        tools.add(AiToolCallingClient.tool("update_order", "Confirm, reject, cancel, deliver, or complete an order after explicit confirmation",
                mapOf(
                        "action", AiToolCallingClient.stringProperty("Action: confirm, reject, cancel, deliver, complete"),
                        "order_id", integerProperty("Internal order ID"),
                        "order_number", AiToolCallingClient.stringProperty("Order number"),
                        "reason", AiToolCallingClient.stringProperty("Reason for reject or cancel"),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "action", "confirmed"));
        tools.add(AiToolCallingClient.tool("query_coupons", "Query coupons",
                mapOf(
                        "name", AiToolCallingClient.stringProperty("Coupon name keyword"),
                        "status", AiToolCallingClient.stringProperty("Coupon status: enabled or disabled"),
                        "page", integerProperty("Page number, default 1"),
                        "page_size", integerProperty("Page size for paged mode"),
                        "all", AiToolCallingClient.booleanProperty("Whether to fetch all matched records")
                )));
        tools.add(AiToolCallingClient.tool("manage_coupon", "Create, update, or delete a coupon after explicit confirmation",
                mapOf(
                        "action", AiToolCallingClient.stringProperty("Action: create, update, delete"),
                        "coupon_id", integerProperty("Coupon ID for update or delete"),
                        "payload", couponPayloadProperty(),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "action", "confirmed"));
        tools.add(AiToolCallingClient.tool("query_employees", "Query employees or fetch employee details",
                mapOf(
                        "employee_id", integerProperty("Employee ID for details"),
                        "name", AiToolCallingClient.stringProperty("Employee name keyword"),
                        "page", integerProperty("Page number, default 1"),
                        "page_size", integerProperty("Page size for paged mode"),
                        "all", AiToolCallingClient.booleanProperty("Whether to fetch all matched records")
                )));
        tools.add(AiToolCallingClient.tool("manage_employee", "Create, update, or enable/disable an employee after explicit confirmation",
                mapOf(
                        "action", AiToolCallingClient.stringProperty("Action: create, update, toggle_status"),
                        "employee_id", integerProperty("Employee ID for update or toggle_status"),
                        "status", AiToolCallingClient.stringProperty("Status for toggle_status: enabled or disabled"),
                        "payload", employeePayloadProperty(),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "action", "confirmed"));
        tools.add(AiToolCallingClient.tool("change_my_password", "Change the current admin password after explicit confirmation",
                mapOf(
                        "old_password", AiToolCallingClient.stringProperty("Current password"),
                        "new_password", AiToolCallingClient.stringProperty("New password"),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "old_password", "new_password", "confirmed"));
        tools.add(AiToolCallingClient.tool("query_categories", "Query categories",
                mapOf(
                        "name", AiToolCallingClient.stringProperty("Category name keyword"),
                        "category_type", AiToolCallingClient.stringProperty("Category type: dish, setmeal, or all"),
                        "page", integerProperty("Page number, default 1"),
                        "page_size", integerProperty("Page size for paged mode"),
                        "all", AiToolCallingClient.booleanProperty("Whether to fetch all matched records")
                )));
        tools.add(AiToolCallingClient.tool("manage_category", "Create, update, delete, or enable/disable a category after explicit confirmation",
                mapOf(
                        "action", AiToolCallingClient.stringProperty("Action: create, update, delete, toggle_status"),
                        "category_id", integerProperty("Category ID for update, delete, or toggle_status"),
                        "status", AiToolCallingClient.stringProperty("Status for toggle_status: enabled or disabled"),
                        "payload", categoryPayloadProperty(),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "action", "confirmed"));
        tools.add(AiToolCallingClient.tool("query_dishes", "Query dishes or fetch dish details",
                mapOf(
                        "dish_id", integerProperty("Dish ID for details"),
                        "name", AiToolCallingClient.stringProperty("Dish name keyword"),
                        "category_id", integerProperty("Category ID"),
                        "status", AiToolCallingClient.stringProperty("Dish status: enabled or disabled"),
                        "page", integerProperty("Page number, default 1"),
                        "page_size", integerProperty("Page size for paged mode"),
                        "all", AiToolCallingClient.booleanProperty("Whether to fetch all matched records")
                )));
        tools.add(AiToolCallingClient.tool("manage_dish", "Create, update, delete, or enable/disable a dish after explicit confirmation",
                mapOf(
                        "action", AiToolCallingClient.stringProperty("Action: create, update, delete, toggle_status"),
                        "dish_id", integerProperty("Dish ID for update, delete, or toggle_status"),
                        "dish_ids", longArrayProperty("Dish IDs for batch delete"),
                        "status", AiToolCallingClient.stringProperty("Status for toggle_status: enabled or disabled"),
                        "payload", dishPayloadProperty(),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "action", "confirmed"));
        tools.add(AiToolCallingClient.tool("query_setmeals", "Query setmeals or fetch setmeal details",
                mapOf(
                        "setmeal_id", integerProperty("Setmeal ID for details"),
                        "name", AiToolCallingClient.stringProperty("Setmeal name keyword"),
                        "category_id", integerProperty("Category ID"),
                        "status", AiToolCallingClient.stringProperty("Setmeal status: enabled or disabled"),
                        "page", integerProperty("Page number, default 1"),
                        "page_size", integerProperty("Page size for paged mode"),
                        "all", AiToolCallingClient.booleanProperty("Whether to fetch all matched records")
                )));
        tools.add(AiToolCallingClient.tool("manage_setmeal", "Create, update, delete, or enable/disable a setmeal after explicit confirmation",
                mapOf(
                        "action", AiToolCallingClient.stringProperty("Action: create, update, delete, toggle_status"),
                        "setmeal_id", integerProperty("Setmeal ID for update, delete, or toggle_status"),
                        "setmeal_ids", longArrayProperty("Setmeal IDs for batch delete"),
                        "status", AiToolCallingClient.stringProperty("Status for toggle_status: enabled or disabled"),
                        "payload", setmealPayloadProperty(),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "action", "confirmed"));
        tools.add(AiToolCallingClient.tool("get_business_overview", "Get today's real-time business overview", Collections.emptyMap()));
        tools.add(AiToolCallingClient.tool("get_business_trend", "Get turnover, order, user, and top goods report JSON",
                mapOf(
                        "range_key", AiToolCallingClient.stringProperty("Range: today, yesterday, last7d, last30d, custom"),
                        "start_date", AiToolCallingClient.stringProperty("Custom range start date in yyyy-MM-dd"),
                        "end_date", AiToolCallingClient.stringProperty("Custom range end date in yyyy-MM-dd")
                )));
        tools.add(AiToolCallingClient.tool("list_operational_documents", "List operational documents used by the admin assistant",
                Collections.emptyMap()));
        tools.add(AiToolCallingClient.tool("search_operational_knowledge", "Search operational knowledge for architecture, delivery process, platform rules, release history, or AI workflow boundaries",
                mapOf(
                        "query", AiToolCallingClient.stringProperty("Natural-language query for internal operational knowledge"),
                        "top_k", integerProperty("Maximum number of matched segments to return")
                ), "query"));
        tools.add(AiToolCallingClient.tool("list_resource_catalog", "List the current internal resource catalog and read-only capabilities",
                Collections.emptyMap()));
        tools.add(AiToolCallingClient.tool("read_resource_detail", "Read a document from the internal resource catalog",
                mapOf(
                        "uri", AiToolCallingClient.stringProperty("Resource URI returned by list_resource_catalog or list_operational_documents")
                ), "uri"));
        tools.add(AiToolCallingClient.tool("describe_upload_capability", "Explain the current chat boundary for admin file upload",
                Collections.emptyMap()));
        return tools;
    }

    private Map<String, Object> integerProperty(String description) {
        return mapOf("type", "integer", "description", description);
    }

    private Map<String, Object> numberProperty(String description) {
        return mapOf("type", "number", "description", description);
    }

    private Map<String, Object> longArrayProperty(String description) {
        return mapOf("type", "array", "description", description, "items", mapOf("type", "integer"));
    }

    private Map<String, Object> categoryPayloadProperty() {
        return mapOf("type", "object", "description", "Category payload", "properties", mapOf(
                "type", AiToolCallingClient.stringProperty("Category type: dish or setmeal"),
                "name", AiToolCallingClient.stringProperty("Category name"),
                "sort", integerProperty("Sort order")
        ), "additionalProperties", false);
    }

    private Map<String, Object> employeePayloadProperty() {
        return mapOf("type", "object", "description", "Employee payload", "properties", mapOf(
                "username", AiToolCallingClient.stringProperty("Login username"),
                "name", AiToolCallingClient.stringProperty("Employee name"),
                "phone", AiToolCallingClient.stringProperty("Phone number"),
                "sex", AiToolCallingClient.stringProperty("Sex field"),
                "id_number", AiToolCallingClient.stringProperty("ID number")
        ), "additionalProperties", false);
    }

    private Map<String, Object> couponPayloadProperty() {
        return mapOf("type", "object", "description", "Coupon payload", "properties", mapOf(
                "name", AiToolCallingClient.stringProperty("Coupon name"),
                "type", integerProperty("Coupon type"),
                "discount_amount", numberProperty("Discount amount"),
                "minimum_amount", numberProperty("Minimum amount"),
                "total_count", integerProperty("Total count"),
                "per_user_limit", integerProperty("Per user limit"),
                "valid_from", AiToolCallingClient.stringProperty("ISO datetime like 2026-07-05T10:00:00"),
                "valid_until", AiToolCallingClient.stringProperty("ISO datetime like 2026-07-31T23:59:59"),
                "status", AiToolCallingClient.stringProperty("enabled or disabled"),
                "description", AiToolCallingClient.stringProperty("Coupon description")
        ), "additionalProperties", true);
    }

    private Map<String, Object> dishPayloadProperty() {
        return mapOf("type", "object", "description", "Dish payload", "properties", mapOf(
                "name", AiToolCallingClient.stringProperty("Dish name"),
                "category_id", integerProperty("Category ID"),
                "price", numberProperty("Dish price"),
                "image", AiToolCallingClient.stringProperty("Image URL"),
                "description", AiToolCallingClient.stringProperty("Dish description"),
                "status", AiToolCallingClient.stringProperty("enabled or disabled"),
                "flavors", mapOf("type", "array", "items", mapOf(
                        "type", "object",
                        "properties", mapOf(
                                "name", AiToolCallingClient.stringProperty("Flavor group name"),
                                "values", mapOf("type", "array", "items", mapOf("type", "string")),
                                "value", AiToolCallingClient.stringProperty("Raw JSON string or plain text flavor value")
                        ),
                        "additionalProperties", true
                ))
        ), "additionalProperties", true);
    }

    private Map<String, Object> setmealPayloadProperty() {
        return mapOf("type", "object", "description", "Setmeal payload", "properties", mapOf(
                "name", AiToolCallingClient.stringProperty("Setmeal name"),
                "category_id", integerProperty("Category ID"),
                "price", numberProperty("Setmeal price"),
                "image", AiToolCallingClient.stringProperty("Image URL"),
                "description", AiToolCallingClient.stringProperty("Setmeal description"),
                "status", AiToolCallingClient.stringProperty("enabled or disabled"),
                "setmeal_dishes", mapOf("type", "array", "items", mapOf(
                        "type", "object",
                        "properties", mapOf(
                                "dish_id", integerProperty("Dish ID"),
                                "name", AiToolCallingClient.stringProperty("Dish name"),
                                "price", numberProperty("Dish price snapshot"),
                                "copies", integerProperty("Number of copies")
                        ),
                        "additionalProperties", true
                ))
        ), "additionalProperties", true);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
