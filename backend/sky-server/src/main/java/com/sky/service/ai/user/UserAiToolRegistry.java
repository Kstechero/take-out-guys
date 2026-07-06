package com.sky.service.ai.user;

import com.sky.service.ai.AiToolCallingClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserAiToolRegistry {

    public List<Map<String, Object>> userTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(AiToolCallingClient.tool("get_shop_status", "Get the current shop opening status", Collections.emptyMap()));
        tools.add(AiToolCallingClient.tool("list_addresses", "List user addresses or get one address detail",
                mapOf(
                        "address_id", integerProperty("Address ID for detail lookup"),
                        "default_only", AiToolCallingClient.booleanProperty("Whether to only return the default address")
                )));
        tools.add(AiToolCallingClient.tool("manage_address", "Create, update, delete, or set default address after explicit user confirmation",
                mapOf(
                        "action", AiToolCallingClient.stringProperty("Action: create, update, delete, set_default"),
                        "address_id", integerProperty("Address ID for update, delete, or set_default"),
                        "payload", addressPayloadProperty(),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "action", "confirmed"));
        tools.add(AiToolCallingClient.tool("list_coupons", "List user coupons, claimable coupons, or order-available coupons",
                mapOf(
                        "scope", AiToolCallingClient.stringProperty("Scope: my, claimable, or order_available"),
                        "status", integerProperty("Optional status for my coupons"),
                        "amount", numberProperty("Order amount for order_available scope"),
                        "order_id", integerProperty("Optional order ID for order_available scope"),
                        "page", integerProperty("Page number for claimable scope"),
                        "page_size", integerProperty("Page size for claimable scope")
                ), "scope"));
        tools.add(AiToolCallingClient.tool("receive_coupon", "Receive a coupon after explicit user confirmation",
                mapOf(
                        "coupon_id", integerProperty("Coupon ID"),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "coupon_id", "confirmed"));
        tools.add(AiToolCallingClient.tool("get_cart", "Get the current shopping cart", Collections.emptyMap()));
        tools.add(AiToolCallingClient.tool("manage_cart", "Add, subtract, or clear shopping cart items after explicit user confirmation for mutations",
                mapOf(
                        "action", AiToolCallingClient.stringProperty("Action: add, sub, clear"),
                        "payload", cartPayloadProperty(),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "action"));
        tools.add(AiToolCallingClient.tool("list_orders", "List recent user orders or fetch one order detail",
                mapOf(
                        "order_id", integerProperty("Order ID for detail lookup"),
                        "order_number", AiToolCallingClient.stringProperty("Order number"),
                        "status", integerProperty("Optional order status filter"),
                        "page", integerProperty("Page number, default 1"),
                        "page_size", integerProperty("Page size, default 10")
                )));
        tools.add(AiToolCallingClient.tool("manage_order", "Cancel, reorder, or remind order after explicit user confirmation",
                mapOf(
                        "action", AiToolCallingClient.stringProperty("Action: cancel, reorder, remind"),
                        "order_id", integerProperty("Order ID"),
                        "order_number", AiToolCallingClient.stringProperty("Order number"),
                        "confirmed", AiToolCallingClient.booleanProperty("Must be true only after the user explicitly confirms the mutation")
                ), "action", "confirmed"));
        tools.add(AiToolCallingClient.tool("search_menu", "Search dishes and setmeals by keyword or category",
                mapOf(
                        "keyword", AiToolCallingClient.stringProperty("Keyword for dish, setmeal, description, or flavor"),
                        "category_id", integerProperty("Optional category ID"),
                        "include_dishes", AiToolCallingClient.booleanProperty("Whether to include dishes"),
                        "include_setmeals", AiToolCallingClient.booleanProperty("Whether to include setmeals")
                )));
        tools.add(AiToolCallingClient.tool("search_service_knowledge", "Search customer-facing service knowledge such as delivery range, after-sales rules, coupon usage, or dine-in pickup guidance",
                mapOf(
                        "query", AiToolCallingClient.stringProperty("Natural-language question about service rules or platform process"),
                        "top_k", integerProperty("Maximum number of matched knowledge chunks")
                ), "query"));
        tools.add(AiToolCallingClient.tool("read_service_resource", "Read a service knowledge document by resource URI",
                mapOf(
                        "uri", AiToolCallingClient.stringProperty("Resource URI returned by search_service_knowledge")
                ), "uri"));
        return tools;
    }

    private Map<String, Object> addressPayloadProperty() {
        return mapOf(
                "type", "object",
                "description", "Address payload",
                "properties", mapOf(
                        "consignee", AiToolCallingClient.stringProperty("Receiver name"),
                        "phone", AiToolCallingClient.stringProperty("Phone number"),
                        "sex", AiToolCallingClient.stringProperty("Sex field"),
                        "province_code", AiToolCallingClient.stringProperty("Province code"),
                        "province_name", AiToolCallingClient.stringProperty("Province name"),
                        "city_code", AiToolCallingClient.stringProperty("City code"),
                        "city_name", AiToolCallingClient.stringProperty("City name"),
                        "district_code", AiToolCallingClient.stringProperty("District code"),
                        "district_name", AiToolCallingClient.stringProperty("District name"),
                        "detail", AiToolCallingClient.stringProperty("Detailed address"),
                        "label", AiToolCallingClient.stringProperty("Address label"),
                        "is_default", AiToolCallingClient.booleanProperty("Whether this address is default")
                ),
                "additionalProperties", true
        );
    }

    private Map<String, Object> cartPayloadProperty() {
        return mapOf(
                "type", "object",
                "description", "Shopping cart payload for add/sub actions",
                "properties", mapOf(
                        "dish_id", integerProperty("Dish ID"),
                        "setmeal_id", integerProperty("Setmeal ID"),
                        "dish_flavor", AiToolCallingClient.stringProperty("Dish flavor"),
                        "number", integerProperty("Quantity hint")
                ),
                "additionalProperties", true
        );
    }

    private Map<String, Object> integerProperty(String description) {
        return mapOf("type", "integer", "description", description);
    }

    private Map<String, Object> numberProperty(String description) {
        return mapOf("type", "number", "description", description);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
