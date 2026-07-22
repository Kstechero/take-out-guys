package com.sky.controller.internal;

import com.sky.context.BaseContext;
import com.sky.dto.InternalAgentCartChangeDTO;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Coupon;
import com.sky.entity.OrderDetail;
import com.sky.entity.ShoppingCart;
import com.sky.result.PageResult;
import com.sky.service.AddressBookService;
import com.sky.service.CouponService;
import com.sky.service.DishService;
import com.sky.service.OrderService;
import com.sky.service.SensitiveWordService;
import com.sky.service.ShoppingCartService;
import com.sky.service.SetmealService;
import com.sky.vo.OrderVO;
import com.sky.vo.SensitiveWordCheckVO;
import com.sky.vo.UserCouponVO;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/internal/agent")
public class InternalAgentUserController {

    private final OrderService orderService;
    private final ShoppingCartService shoppingCartService;
    private final AddressBookService addressBookService;
    private final CouponService couponService;
    private final DishService dishService;
    private final SetmealService setmealService;
    private final SensitiveWordService sensitiveWordService;
    private final RedisTemplate redisTemplate;

    public InternalAgentUserController(OrderService orderService,
                                       ShoppingCartService shoppingCartService,
                                       AddressBookService addressBookService,
                                       CouponService couponService,
                                       SensitiveWordService sensitiveWordService,
                                       RedisTemplate redisTemplate,
                                       DishService dishService,
                                       SetmealService setmealService) {
        this.orderService = orderService;
        this.shoppingCartService = shoppingCartService;
        this.addressBookService = addressBookService;
        this.couponService = couponService;
        this.sensitiveWordService = sensitiveWordService;
        this.redisTemplate = redisTemplate;
        this.dishService = dishService;
        this.setmealService = setmealService;
    }

    @GetMapping("/orders/recent")
    public Map<String, Object> recentOrders(@RequestHeader("X-Request-Id") String requestId,
                                             @RequestHeader("X-Actor-Type") String actorType,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "5") Integer limit) {
        requireUser(actorType);
        int safeLimit = Math.max(1, Math.min(limit == null ? 5 : limit, 10));
        PageResult page = orderService.pageQueryForUser(1, safeLimit, status);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object record : records(page)) {
            if (record instanceof OrderVO) {
                items.add(orderSummary((OrderVO) record));
            }
        }
        return success(mapOf("items", items, "total", page == null ? 0 : page.getTotal()), requestId);
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, Object> orderDetail(@RequestHeader("X-Request-Id") String requestId,
                                           @RequestHeader("X-Actor-Type") String actorType,
                                           @PathVariable Long orderId) {
        requireUser(actorType);
        return success(orderDetail(orderService.details(orderId, true)), requestId);
    }

    @GetMapping("/cart")
    public Map<String, Object> cart(@RequestHeader("X-Request-Id") String requestId,
                                    @RequestHeader("X-Actor-Type") String actorType) {
        requireUser(actorType);
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (ShoppingCart cart : nullSafe(shoppingCartService.showShoppingCart())) {
            int quantity = cart.getNumber() == null ? 0 : cart.getNumber();
            BigDecimal unitAmount = cart.getAmount() == null ? BigDecimal.ZERO : cart.getAmount();
            items.add(mapOf(
                    "id", cart.getId(),
                    "dish_id", cart.getDishId(),
                    "setmeal_id", cart.getSetmealId(),
                    "name", cart.getName(),
                    "flavor", cart.getDishFlavor(),
                    "quantity", quantity,
                    "unit_amount", unitAmount,
                    "image", cart.getImage()
            ));
            totalQuantity += quantity;
            totalAmount = totalAmount.add(unitAmount.multiply(BigDecimal.valueOf(quantity)));
        }
        return success(mapOf("items", items, "total_quantity", totalQuantity, "total_amount", totalAmount), requestId);
    }

    @GetMapping("/addresses")
    public Map<String, Object> addresses(@RequestHeader("X-Request-Id") String requestId,
                                         @RequestHeader("X-Actor-Type") String actorType,
                                         @RequestParam(defaultValue = "false", name = "default_only") Boolean defaultOnly) {
        requireUser(actorType);
        AddressBook probe = new AddressBook();
        probe.setUserId(BaseContext.getCurrentId());
        if (Boolean.TRUE.equals(defaultOnly)) {
            probe.setIsDefault(1);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (AddressBook address : nullSafe(addressBookService.list(probe))) {
            items.add(mapOf(
                    "id", address.getId(),
                    "consignee_masked", maskName(address.getConsignee()),
                    "phone_masked", maskPhone(address.getPhone()),
                    "region", joinRegion(address),
                    "detail_masked", StringUtils.hasText(address.getDetail()) ? "***" : "",
                    "label", address.getLabel(),
                    "is_default", Integer.valueOf(1).equals(address.getIsDefault())
            ));
        }
        return success(mapOf("items", items), requestId);
    }

    @GetMapping("/coupons/available")
    public Map<String, Object> availableCoupons(@RequestHeader("X-Request-Id") String requestId,
                                                 @RequestHeader("X-Actor-Type") String actorType,
                                                 @RequestParam(required = false, name = "order_amount") BigDecimal orderAmount) {
        requireUser(actorType);
        List<Map<String, Object>> items = new ArrayList<>();
        if (orderAmount != null) {
            if (orderAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("order_amount must not be negative");
            }
            for (Coupon coupon : nullSafe(couponService.availableForOrder(orderAmount, null))) {
                items.add(couponItem(coupon));
            }
        } else {
            for (UserCouponVO coupon : nullSafe(couponService.myCoupons(0))) {
                items.add(userCouponItem(coupon));
            }
        }
        return success(mapOf("items", items, "order_amount", orderAmount), requestId);
    }

    @PostMapping("/sensitive-words/check")
    public Map<String, Object> checkSensitiveWords(@RequestHeader("X-Request-Id") String requestId,
                                                    @RequestHeader("X-Actor-Type") String actorType,
                                                    @RequestBody Map<String, String> body) {
        requireUserOrAdmin(actorType);
        String text = body == null ? null : body.get("text");
        if (!StringUtils.hasText(text) || text.length() > 2000) {
            throw new IllegalArgumentException("text must contain 1 to 2000 characters");
        }
        SensitiveWordCheckVO result = sensitiveWordService.scanText(text);
        return success(mapOf(
                "safe", result == null || !Boolean.TRUE.equals(result.getHit()),
                "masked_text", result == null ? text : result.getContent()
        ), requestId);
    }

    @PostMapping("/reviews/draft/check")
    public Map<String, Object> checkReviewDraft(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestBody Map<String, Object> body) {
        requireUser(actorType);
        if (body == null || body.get("order_id") == null || body.get("dish_id") == null) {
            throw new IllegalArgumentException("order_id and dish_id are required");
        }
        long orderId = Long.parseLong(String.valueOf(body.get("order_id")));
        long dishId = Long.parseLong(String.valueOf(body.get("dish_id")));
        int rating = body.get("rating") == null ? 5 : Integer.parseInt(String.valueOf(body.get("rating")));
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        OrderVO order = orderService.details(orderId, true);
        if (order == null || !Integer.valueOf(5).equals(order.getStatus())) {
            throw new IllegalStateException("Only completed orders can be reviewed");
        }
        OrderDetail matched = null;
        for (OrderDetail item : nullSafe(order.getOrderDetailList())) {
            if (Long.valueOf(dishId).equals(item.getDishId())) {
                matched = item;
                break;
            }
        }
        if (matched == null) {
            throw new SecurityException("Dish is not part of the owned order");
        }
        String highlights = body.get("highlights") == null ? "" : String.valueOf(body.get("highlights")).trim();
        if (highlights.length() > 500) {
            throw new IllegalArgumentException("highlights must not exceed 500 characters");
        }
        SensitiveWordCheckVO scan = StringUtils.hasText(highlights)
                ? sensitiveWordService.scanText(highlights) : null;
        boolean safe = scan == null || !Boolean.TRUE.equals(scan.getHit());
        return success(mapOf(
                "eligible", true,
                "safe", safe,
                "order_id", orderId,
                "dish_id", dishId,
                "dish_name", matched.getName(),
                "rating", rating,
                "highlights", scan == null ? highlights : scan.getContent(),
                "instruction", safe
                        ? "Generate a concise review draft; do not claim it has been published."
                        : "Sensitive content detected; ask the user to revise it before drafting."
        ), requestId);
    }

    @PostMapping("/cart/changes")
    public Map<String, Object> changeCart(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Confirmation-Token") String confirmationToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody InternalAgentCartChangeDTO body) {
        requireUser(actorType);
        String replayStatus = beginIdempotentWrite(confirmationToken, idempotencyKey);
        if (replayStatus != null) {
            return success(mapOf("status", replayStatus), requestId);
        }
        try {
            validateReviewedPrice(body);
            applyCartChange(body);
            completeIdempotentWrite(idempotencyKey);
            return success(mapOf("status", "APPLIED", "action", body.getAction()), requestId);
        } catch (RuntimeException ex) {
            failIdempotentWrite(idempotencyKey);
            throw ex;
        }
    }

    @PostMapping("/coupons/{couponId}/claim")
    public Map<String, Object> claimCoupon(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Confirmation-Token") String confirmationToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable Long couponId) {
        requireUser(actorType);
        if (couponId == null || couponId <= 0) {
            throw new IllegalArgumentException("couponId must be positive");
        }
        String replayStatus = beginIdempotentWrite(confirmationToken, idempotencyKey);
        if (replayStatus != null) {
            return success(mapOf("status", replayStatus), requestId);
        }
        try {
            couponService.receive(couponId);
            completeIdempotentWrite(idempotencyKey);
            return success(mapOf("status", "CLAIMED", "coupon_id", couponId), requestId);
        } catch (RuntimeException ex) {
            failIdempotentWrite(idempotencyKey);
            throw ex;
        }
    }

    private void applyCartChange(InternalAgentCartChangeDTO body) {
        if (body == null || !StringUtils.hasText(body.getAction())) {
            throw new IllegalArgumentException("action is required");
        }
        switch (body.getAction()) {
            case "add":
                requireProduct(body);
                int addQuantity = requireQuantity(body.getQuantity());
                ShoppingCartDTO add = cartDto(body.getDishId(), body.getSetmealId(), null, body.getFlavor());
                for (int i = 0; i < addQuantity; i++) shoppingCartService.addShoppingCart(add);
                return;
            case "update":
                ShoppingCart current = requireCartItem(body.getCartItemId());
                int target = requireQuantity(body.getQuantity());
                int existing = current.getNumber() == null ? 0 : current.getNumber();
                ShoppingCartDTO update = cartDto(current.getDishId(), current.getSetmealId(),
                        current.getId(), body.getFlavor() == null ? current.getDishFlavor() : body.getFlavor());
                for (int i = existing; i < target; i++) shoppingCartService.addShoppingCart(update);
                for (int i = existing; i > target; i--) shoppingCartService.subShoppingCart(update);
                return;
            case "remove":
                ShoppingCart removed = requireCartItem(body.getCartItemId());
                ShoppingCartDTO remove = cartDto(removed.getDishId(), removed.getSetmealId(),
                        removed.getId(), removed.getDishFlavor());
                int count = removed.getNumber() == null ? 1 : removed.getNumber();
                for (int i = 0; i < count; i++) shoppingCartService.subShoppingCart(remove);
                return;
            case "clear":
                shoppingCartService.cleanShoppingCart();
                return;
            default:
                throw new IllegalArgumentException("unsupported cart action");
        }
    }

    private void requireProduct(InternalAgentCartChangeDTO body) {
        if ((body.getDishId() == null) == (body.getSetmealId() == null)) {
            throw new IllegalArgumentException("exactly one product is required");
        }
    }

    private void validateReviewedPrice(InternalAgentCartChangeDTO body) {
        if (body == null || !"add".equals(body.getAction())) return;
        requireProduct(body);
        if (body.getExpectedUnitAmount() == null) {
            throw new IllegalArgumentException("expected_unit_amount is required");
        }
        BigDecimal current;
        Integer status;
        if (body.getDishId() != null) {
            com.sky.vo.DishVO dish = dishService.getByIdWithFlavor(body.getDishId());
            if (dish == null) throw new IllegalArgumentException("dish not found");
            current = dish.getPrice();
            status = dish.getStatus();
        } else {
            com.sky.vo.SetmealVO setmeal = setmealService.getByIdWithDish(body.getSetmealId());
            if (setmeal == null) throw new IllegalArgumentException("setmeal not found");
            current = setmeal.getPrice();
            status = setmeal.getStatus();
        }
        if (!Integer.valueOf(1).equals(status) || current == null
                || current.compareTo(body.getExpectedUnitAmount()) != 0) {
            throw new IllegalStateException("Product price or availability changed after preview");
        }
    }

    private int requireQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > 20) {
            throw new IllegalArgumentException("quantity must be between 1 and 20");
        }
        return quantity;
    }

    private ShoppingCart requireCartItem(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("cart_item_id is required");
        for (ShoppingCart item : nullSafe(shoppingCartService.showShoppingCart())) {
            if (id.equals(item.getId())) return item;
        }
        throw new SecurityException("Cart item not found or forbidden");
    }

    private ShoppingCartDTO cartDto(Long dishId, Long setmealId, Long id, String flavor) {
        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setDishId(dishId);
        dto.setSetmealId(setmealId);
        dto.setId(id);
        dto.setDishFlavor(flavor);
        return dto;
    }

    private String beginIdempotentWrite(String confirmationToken, String idempotencyKey) {
        if (!StringUtils.hasText(confirmationToken) || confirmationToken.length() < 20
                || !StringUtils.hasText(idempotencyKey) || idempotencyKey.length() < 20) {
            throw new SecurityException("Valid confirmation and idempotency keys are required");
        }
        String key = idempotencyRedisKey(idempotencyKey);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                key, "PROCESSING", 24, TimeUnit.HOURS);
        if (Boolean.TRUE.equals(acquired)) return null;
        Object state = redisTemplate.opsForValue().get(key);
        return "COMPLETED".equals(state) ? "REPLAYED" : "ALREADY_PROCESSING";
    }

    private void completeIdempotentWrite(String idempotencyKey) {
        redisTemplate.opsForValue().set(
                idempotencyRedisKey(idempotencyKey), "COMPLETED", 24, TimeUnit.HOURS);
    }

    private void failIdempotentWrite(String idempotencyKey) {
        redisTemplate.opsForValue().set(
                idempotencyRedisKey(idempotencyKey), "FAILED_UNKNOWN", 24, TimeUnit.HOURS);
    }

    private String idempotencyRedisKey(String idempotencyKey) {
        return "agent:write:" + BaseContext.getCurrentId() + ":" + idempotencyKey;
    }

    private Map<String, Object> orderSummary(OrderVO order) {
        return mapOf(
                "id", order.getId(),
                "number", order.getNumber(),
                "status", order.getStatus(),
                "status_label", orderStatus(order.getStatus()),
                "amount", order.getAmount(),
                "order_time", order.getOrderTime(),
                "estimated_delivery_time", order.getEstimatedDeliveryTime(),
                "items_summary", order.getOrderDishes()
        );
    }

    private Map<String, Object> orderDetail(OrderVO order) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderDetail detail : nullSafe(order.getOrderDetailList())) {
            items.add(mapOf(
                    "dish_id", detail.getDishId(),
                    "setmeal_id", detail.getSetmealId(),
                    "name", detail.getName(),
                    "flavor", detail.getDishFlavor(),
                    "quantity", detail.getNumber(),
                    "unit_amount", detail.getAmount()
            ));
        }
        Map<String, Object> result = orderSummary(order);
        result.put("pay_status", order.getPayStatus());
        result.put("original_amount", order.getOriginalAmount());
        result.put("discount_amount", order.getDiscountAmount());
        result.put("items", items);
        return result;
    }

    private Map<String, Object> couponItem(Coupon coupon) {
        return mapOf(
                "coupon_id", coupon.getId(),
                "name", coupon.getName(),
                "discount_amount", coupon.getDiscountAmount(),
                "minimum_amount", coupon.getMinimumAmount(),
                "valid_until", coupon.getValidUntil(),
                "description", coupon.getDescription()
        );
    }

    private Map<String, Object> userCouponItem(UserCouponVO coupon) {
        return mapOf(
                "user_coupon_id", coupon.getUserCouponId(),
                "coupon_id", coupon.getCouponId(),
                "name", coupon.getName(),
                "discount_amount", coupon.getDiscountAmount(),
                "minimum_amount", coupon.getMinimumAmount(),
                "valid_until", coupon.getValidUntil(),
                "description", coupon.getDescription()
        );
    }

    private String orderStatus(Integer status) {
        if (status == null) return "UNKNOWN";
        switch (status) {
            case 1: return "PENDING_PAYMENT";
            case 2: return "PENDING_ACCEPTANCE";
            case 3: return "ACCEPTED";
            case 4: return "DELIVERING";
            case 5: return "COMPLETED";
            case 6: return "CANCELLED";
            default: return "UNKNOWN";
        }
    }

    private String maskName(String value) {
        return StringUtils.hasText(value) ? value.substring(0, 1) + "**" : "";
    }

    private String maskPhone(String value) {
        if (!StringUtils.hasText(value) || value.length() < 7) {
            return "***";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private String joinRegion(AddressBook address) {
        return value(address.getProvinceName()) + value(address.getCityName()) + value(address.getDistrictName());
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private void requireUser(String actorType) {
        if (!"user".equals(actorType)) {
            throw new SecurityException("User actor required");
        }
    }

    private void requireUserOrAdmin(String actorType) {
        if (!("user".equals(actorType) || "admin".equals(actorType))) {
            throw new SecurityException("Valid actor required");
        }
    }

    private List<?> records(PageResult page) {
        return page == null || page.getRecords() == null ? Collections.emptyList() : page.getRecords();
    }

    private <T> List<T> nullSafe(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }

    private Map<String, Object> success(Object data, String requestId) {
        return mapOf("ok", true, "data", data, "error_code", null, "message", "", "request_id", requestId);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
