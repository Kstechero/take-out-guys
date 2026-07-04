package com.sky.service.ai.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.constant.StatusConstant;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Coupon;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.OrderService;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import com.sky.vo.OrderVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class UserAiToolExecutor {

    private static final int ORDER_LOOKBACK = 50;
    private static final int MENU_LIMIT = 50;

    private final ObjectMapper objectMapper;
    private final com.sky.controller.user.ShopController shopController;
    private final com.sky.controller.user.AddressBookController addressBookController;
    private final com.sky.controller.user.CouponController couponController;
    private final com.sky.controller.user.ShoppingCartController shoppingCartController;
    private final com.sky.controller.user.OrderController orderController;
    private final DishService dishService;
    private final SetmealService setmealService;
    private final OrderService orderService;

    public UserAiToolExecutor(ObjectMapper objectMapper,
                              com.sky.controller.user.ShopController shopController,
                              com.sky.controller.user.AddressBookController addressBookController,
                              com.sky.controller.user.CouponController couponController,
                              com.sky.controller.user.ShoppingCartController shoppingCartController,
                              com.sky.controller.user.OrderController orderController,
                              DishService dishService,
                              SetmealService setmealService,
                              OrderService orderService) {
        this.objectMapper = objectMapper;
        this.shopController = shopController;
        this.addressBookController = addressBookController;
        this.couponController = couponController;
        this.shoppingCartController = shoppingCartController;
        this.orderController = orderController;
        this.dishService = dishService;
        this.setmealService = setmealService;
        this.orderService = orderService;
    }

    public String execute(String name, JsonNode args) throws Exception {
        switch (name) {
            case "get_shop_status":
                return json(getShopStatusPayload());
            case "list_addresses":
                return json(listAddressesPayload(args));
            case "manage_address":
                return json(manageAddressPayload(args));
            case "list_coupons":
                return json(listCouponsPayload(args));
            case "receive_coupon":
                return json(receiveCouponPayload(args));
            case "get_cart":
                return json(getCartPayload());
            case "manage_cart":
                return json(manageCartPayload(args));
            case "list_orders":
                return json(listOrdersPayload(args));
            case "manage_order":
                return json(manageOrderPayload(args));
            case "search_menu":
                return json(searchMenuPayload(args));
            default:
                throw new IllegalArgumentException("Unknown user tool: " + name);
        }
    }

    private Object getShopStatusPayload() {
        return shopController.getStatus();
    }

    private Object listAddressesPayload(JsonNode args) {
        Long addressId = parseLong(args, "address_id");
        if (addressId != null) {
            return addressBookController.getById(addressId);
        }
        if (booleanArg(args, false, "default_only")) {
            return addressBookController.getDefault();
        }
        return addressBookController.list();
    }

    private Object manageAddressPayload(JsonNode args) {
        requireConfirmed(args);
        String action = normalize(args.path("action").asText());

        if ("create".equals(action)) {
            AddressBook addressBook = buildAddressBook(requiredObject(args, "payload"));
            return mapOf("operation", addressBookController.save(addressBook), "current", addressBookController.list());
        }

        Long addressId = requiredLong(args, "address_id");
        if ("update".equals(action)) {
            AddressBook addressBook = buildAddressBook(requiredObject(args, "payload"));
            addressBook.setId(addressId);
            return mapOf("operation", addressBookController.update(addressBook), "details", addressBookController.getById(addressId));
        }
        if ("delete".equals(action)) {
            return mapOf("operation", addressBookController.deleteById(addressId), "current", addressBookController.list());
        }
        if ("set_default".equals(action)) {
            AddressBook addressBook = new AddressBook();
            addressBook.setId(addressId);
            return mapOf("operation", addressBookController.setDefault(addressBook), "current", addressBookController.getDefault());
        }
        throw new IllegalArgumentException("action must be create, update, delete, or set_default");
    }

    private Object listCouponsPayload(JsonNode args) {
        String scope = normalize(args.path("scope").asText());
        if ("my".equals(scope)) {
            Integer status = parseInteger(args, "status");
            return couponController.myCoupons(status);
        }
        if ("claimable".equals(scope)) {
            int page = intArg(args, 1, "page");
            int pageSize = intArg(args, 20, "page_size", "pageSize");
            return couponController.available(page, pageSize);
        }
        if ("order_available".equals(scope)) {
            BigDecimal amount = requiredDecimal(args, "amount");
            Long orderId = parseLong(args, "order_id", "orderId");
            return couponController.availableForOrder(amount, orderId);
        }
        throw new IllegalArgumentException("scope must be my, claimable, or order_available");
    }

    private Object receiveCouponPayload(JsonNode args) {
        requireConfirmed(args);
        Long couponId = requiredLong(args, "coupon_id");
        return mapOf(
                "operation", couponController.receive(couponId),
                "myCoupons", couponController.myCoupons(null)
        );
    }

    private Object getCartPayload() {
        return shoppingCartController.list();
    }

    private Object manageCartPayload(JsonNode args) {
        requireConfirmed(args);
        String action = normalize(args.path("action").asText());

        if ("clear".equals(action)) {
            return mapOf("operation", shoppingCartController.clean(), "current", shoppingCartController.list());
        }

        ShoppingCartDTO dto = buildShoppingCartDTO(requiredObject(args, "payload"));
        if ("add".equals(action)) {
            return mapOf("operation", shoppingCartController.add(dto), "current", shoppingCartController.list());
        }
        if ("sub".equals(action)) {
            return mapOf("operation", shoppingCartController.sub(dto), "current", shoppingCartController.list());
        }
        throw new IllegalArgumentException("action must be add, sub, or clear");
    }

    private Object listOrdersPayload(JsonNode args) {
        Long orderId = parseLong(args, "order_id");
        if (orderId != null) {
            return orderController.details(orderId);
        }

        String orderNumber = textArg(args, "order_number");
        if (StringUtils.hasText(orderNumber)) {
            OrderVO order = resolveOrder(args);
            return Result.success(order);
        }

        int page = intArg(args, 1, "page");
        int pageSize = intArg(args, 10, "page_size", "pageSize");
        Integer status = parseInteger(args, "status");
        return orderController.historyOrders(page, pageSize, status);
    }

    private Object manageOrderPayload(JsonNode args) throws Exception {
        requireConfirmed(args);
        String action = normalize(args.path("action").asText());
        OrderVO order = resolveOrder(args);

        if ("cancel".equals(action)) {
            return mapOf("operation", orderController.cancel(order.getId()), "details", orderController.details(order.getId()));
        }
        if ("reorder".equals(action)) {
            return mapOf("operation", orderController.repetition(order.getId()), "cart", shoppingCartController.list());
        }
        if ("remind".equals(action)) {
            return mapOf("operation", orderController.reminder(order.getId()), "details", orderController.details(order.getId()));
        }
        throw new IllegalArgumentException("action must be cancel, reorder, or remind");
    }

    private Object searchMenuPayload(JsonNode args) {
        String keyword = normalizeKeyword(textArg(args, "keyword"));
        Long categoryId = parseLong(args, "category_id", "categoryId");
        boolean includeDishes = booleanArg(args, true, "include_dishes", "includeDishes");
        boolean includeSetmeals = booleanArg(args, true, "include_setmeals", "includeSetmeals");

        List<DishVO> dishes = Collections.emptyList();
        if (includeDishes) {
            dishes = filterDishes(keyword, categoryId);
        }

        List<Setmeal> setmeals = Collections.emptyList();
        if (includeSetmeals) {
            setmeals = filterSetmeals(keyword, categoryId);
        }

        return mapOf(
                "keyword", keyword,
                "categoryId", categoryId,
                "dishes", dishes,
                "setmeals", setmeals
        );
    }

    private List<DishVO> filterDishes(String keyword, Long categoryId) {
        List<DishVO> result = new ArrayList<>();
        if (categoryId != null) {
            Dish probe = new Dish();
            probe.setCategoryId(categoryId);
            probe.setStatus(StatusConstant.ENABLE);
            result.addAll(dishService.listWithFlavor(probe));
        } else {
            Dish probe = new Dish();
            probe.setStatus(StatusConstant.ENABLE);
            result.addAll(dishService.listWithFlavor(probe));
        }
        if (!StringUtils.hasText(keyword)) {
            return limit(result, MENU_LIMIT);
        }
        List<DishVO> filtered = new ArrayList<>();
        for (DishVO dish : result) {
            if (containsMenuKeyword(dish.getName(), keyword)
                    || containsMenuKeyword(dish.getDescription(), keyword)
                    || containsAnyFlavor(dish, keyword)) {
                filtered.add(dish);
            }
        }
        return limit(filtered, MENU_LIMIT);
    }

    private List<Setmeal> filterSetmeals(String keyword, Long categoryId) {
        Setmeal probe = new Setmeal();
        probe.setStatus(StatusConstant.ENABLE);
        if (categoryId != null) {
            probe.setCategoryId(categoryId);
        }
        List<Setmeal> result = setmealService.list(probe);
        if (!StringUtils.hasText(keyword)) {
            return limit(result, MENU_LIMIT);
        }
        List<Setmeal> filtered = new ArrayList<>();
        for (Setmeal setmeal : result) {
            if (containsMenuKeyword(setmeal.getName(), keyword) || containsMenuKeyword(setmeal.getDescription(), keyword)) {
                filtered.add(setmeal);
            }
        }
        return limit(filtered, MENU_LIMIT);
    }

    private boolean containsAnyFlavor(DishVO dish, String keyword) {
        if (dish.getFlavors() == null) {
            return false;
        }
        for (com.sky.entity.DishFlavor flavor : dish.getFlavors()) {
            if (containsMenuKeyword(flavor.getName(), keyword) || containsMenuKeyword(flavor.getValue(), keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsMenuKeyword(String source, String keyword) {
        return StringUtils.hasText(source) && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private <T> List<T> limit(List<T> source, int max) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.size() <= max ? source : new ArrayList<>(source.subList(0, max));
    }

    private OrderVO resolveOrder(JsonNode args) {
        Long orderId = parseLong(args, "order_id");
        if (orderId != null) {
            Result<OrderVO> result = orderController.details(orderId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        }

        String orderNumber = textArg(args, "order_number");
        if (!StringUtils.hasText(orderNumber)) {
            throw new IllegalArgumentException("order_id or order_number is required");
        }

        PageResult pageResult = orderService.pageQueryForUser(1, ORDER_LOOKBACK, null);
        List<OrderVO> orders = castOrderVOs(pageResult);
        for (OrderVO order : orders) {
            if (order == null) {
                continue;
            }
            if (orderNumber.equals(String.valueOf(order.getId())) || orderNumber.equals(order.getNumber())) {
                return order;
            }
        }
        throw new IllegalArgumentException("Order not found");
    }

    private List<OrderVO> castOrderVOs(PageResult pageResult) {
        if (pageResult == null || pageResult.getRecords() == null) {
            return Collections.emptyList();
        }
        List<OrderVO> result = new ArrayList<>();
        for (Object record : pageResult.getRecords()) {
            result.add(objectMapper.convertValue(record, OrderVO.class));
        }
        return result;
    }

    private AddressBook buildAddressBook(JsonNode payload) {
        AddressBook addressBook = new AddressBook();
        addressBook.setConsignee(requiredText(payload, "consignee"));
        addressBook.setPhone(requiredText(payload, "phone"));
        addressBook.setSex(textArg(payload, "sex"));
        addressBook.setProvinceCode(textArg(payload, "province_code", "provinceCode"));
        addressBook.setProvinceName(textArg(payload, "province_name", "provinceName"));
        addressBook.setCityCode(textArg(payload, "city_code", "cityCode"));
        addressBook.setCityName(textArg(payload, "city_name", "cityName"));
        addressBook.setDistrictCode(textArg(payload, "district_code", "districtCode"));
        addressBook.setDistrictName(textArg(payload, "district_name", "districtName"));
        addressBook.setDetail(requiredText(payload, "detail"));
        addressBook.setLabel(textArg(payload, "label"));
        if (firstPresent(payload, "is_default", "isDefault") != null) {
            addressBook.setIsDefault(booleanArg(payload, false, "is_default", "isDefault") ? 1 : 0);
        }
        return addressBook;
    }

    private ShoppingCartDTO buildShoppingCartDTO(JsonNode payload) {
        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setId(parseLong(payload, "id"));
        dto.setDishId(parseLong(payload, "dish_id", "dishId"));
        dto.setSetmealId(parseLong(payload, "setmeal_id", "setmealId"));
        dto.setDishFlavor(textArg(payload, "dish_flavor", "dishFlavor"));
        if (dto.getDishId() == null && dto.getSetmealId() == null && dto.getId() == null) {
            throw new IllegalArgumentException("dish_id or setmeal_id is required");
        }
        return dto;
    }

    private void requireConfirmed(JsonNode args) {
        if (!booleanArg(args, false, "confirmed")) {
            throw new IllegalArgumentException("This tool mutates backend data and requires confirmed=true after explicit user confirmation");
        }
    }

    private JsonNode firstPresent(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode candidate = node.get(field);
            if (candidate != null && !candidate.isNull()) {
                return candidate;
            }
        }
        return null;
    }

    private String textArg(JsonNode node, String... fields) {
        JsonNode target = firstPresent(node, fields);
        if (target == null) {
            return null;
        }
        String value = target.asText(null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requiredText(JsonNode node, String... fields) {
        String value = textArg(node, fields);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fields[0] + " is required");
        }
        return value;
    }

    private boolean booleanArg(JsonNode node, boolean defaultValue, String... fields) {
        JsonNode target = firstPresent(node, fields);
        if (target == null) {
            return defaultValue;
        }
        if (target.isBoolean()) {
            return target.asBoolean();
        }
        String text = target.asText();
        return StringUtils.hasText(text) ? "true".equalsIgnoreCase(text.trim()) : defaultValue;
    }

    private Long parseLong(JsonNode node, String... fields) {
        JsonNode target = firstPresent(node, fields);
        if (target == null) {
            return null;
        }
        if (target.isNumber()) {
            return target.asLong();
        }
        String text = target.asText();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long requiredLong(JsonNode node, String... fields) {
        Long value = parseLong(node, fields);
        if (value == null) {
            throw new IllegalArgumentException(fields[0] + " is required");
        }
        return value;
    }

    private Integer parseInteger(JsonNode node, String... fields) {
        JsonNode target = firstPresent(node, fields);
        if (target == null) {
            return null;
        }
        if (target.isInt() || target.isLong()) {
            return target.asInt();
        }
        String text = target.asText();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int intArg(JsonNode node, int defaultValue, String... fields) {
        Integer value = parseInteger(node, fields);
        return value == null ? defaultValue : value;
    }

    private BigDecimal parseDecimal(JsonNode node, String... fields) {
        JsonNode target = firstPresent(node, fields);
        if (target == null) {
            return null;
        }
        if (target.isNumber()) {
            return target.decimalValue();
        }
        String text = target.asText();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal requiredDecimal(JsonNode node, String... fields) {
        BigDecimal value = parseDecimal(node, fields);
        if (value == null) {
            throw new IllegalArgumentException(fields[0] + " is required");
        }
        return value;
    }

    private JsonNode requiredObject(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isObject()) {
            throw new IllegalArgumentException(field + " must be an object");
        }
        return value;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeKeyword(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
