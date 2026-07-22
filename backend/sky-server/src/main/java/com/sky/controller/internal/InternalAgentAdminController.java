package com.sky.controller.internal;

import com.sky.context.BaseContext;
import com.sky.dto.AdminReviewPageQueryDTO;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.dto.CouponPageQueryDTO;
import com.sky.dto.CouponDTO;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.InternalAgentAdminCategoryCreateDTO;
import com.sky.dto.InternalAgentAdminCouponActionDTO;
import com.sky.dto.InternalAgentAdminCouponCreateDTO;
import com.sky.dto.InternalAgentAdminDishMutationDTO;
import com.sky.dto.InternalAgentAdminOrderActionDTO;
import com.sky.dto.InternalAgentAdminShopStatusDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Coupon;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.properties.AgentServiceProperties;
import com.sky.service.CouponService;
import com.sky.service.CategoryService;
import com.sky.service.DishService;
import com.sky.service.OrderService;
import com.sky.service.ReviewService;
import com.sky.service.SetmealService;
import com.sky.service.WorkspaceService;
import com.sky.vo.AdminReviewPageVO;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.OrderVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;

@RestController
@RequestMapping("/internal/agent/admin")
@Slf4j
public class InternalAgentAdminController {

    private static final String SHOP_STATUS_KEY = "SHOP_STATUS";

    private final OrderService orderService;
    private final CategoryService categoryService;
    private final DishService dishService;
    private final SetmealService setmealService;
    private final CouponService couponService;
    private final ReviewService reviewService;
    private final WorkspaceService workspaceService;
    private final RedisTemplate redisTemplate;
    private final AgentServiceProperties agentServiceProperties;

    public InternalAgentAdminController(OrderService orderService,
                                        CategoryService categoryService,
                                        DishService dishService,
                                        SetmealService setmealService,
                                        CouponService couponService,
                                        ReviewService reviewService,
                                        WorkspaceService workspaceService,
                                        RedisTemplate redisTemplate,
                                        AgentServiceProperties agentServiceProperties) {
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.dishService = dishService;
        this.setmealService = setmealService;
        this.couponService = couponService;
        this.reviewService = reviewService;
        this.workspaceService = workspaceService;
        this.redisTemplate = redisTemplate;
        this.agentServiceProperties = agentServiceProperties;
    }

    @GetMapping("/business/overview")
    public Map<String, Object> businessOverview(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestParam(required = false) String begin,
            @RequestParam(required = false) String end) {
        requireAdmin(actorType, roles);
        LocalDate beginDate = parseDate(begin, LocalDate.now());
        LocalDate endDate = parseDate(end, beginDate);
        if (beginDate.isAfter(endDate)) throw new IllegalArgumentException("begin must not be after end");
        BusinessDataVO business = workspaceService.getBusinessData(
                beginDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        OrderOverViewVO orders = workspaceService.getOrderOverview();
        return success(mapOf(
                "begin", beginDate.toString(),
                "end", endDate.toString(),
                "turnover", business.getTurnover(),
                "valid_order_count", business.getValidOrderCount(),
                "order_completion_rate", business.getOrderCompletionRate(),
                "unit_price", business.getUnitPrice(),
                "new_users", business.getNewUsers(),
                "waiting_orders", orders.getWaitingOrders(),
                "delivered_orders", orders.getDeliveredOrders(),
                "completed_orders", orders.getCompletedOrders(),
                "cancelled_orders", orders.getCancelledOrders(),
                "all_orders", orders.getAllOrders(),
                "generated_at", LocalDateTime.now().toString(),
                "scope", "single_store,date=" + beginDate + ".." + endDate,
                "source", "spring_internal_api"
        ), requestId);
    }

    @GetMapping("/orders")
    public Map<String, Object> orders(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String begin,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "10") Integer limit) {
        requireAdmin(actorType, roles);
        if (!StringUtils.hasText(number) && status == null
                && !StringUtils.hasText(begin) && !StringUtils.hasText(end)) {
            throw new IllegalArgumentException("bounded order filter required");
        }
        OrdersPageQueryDTO query = new OrdersPageQueryDTO();
        query.setPage(1);
        query.setPageSize(safeLimit(limit));
        query.setNumber(number);
        query.setStatus(status);
        if (StringUtils.hasText(begin)) query.setBeginTime(parseDate(begin, null).atStartOfDay());
        if (StringUtils.hasText(end)) query.setEndTime(parseDate(end, null).atTime(LocalTime.MAX));
        PageResult page = orderService.conditionSearch(query);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object record : records(page)) {
            if (record instanceof OrderVO) items.add(orderSummary((OrderVO) record));
        }
        return success(mapOf(
                "items", items,
                "total", page == null ? 0 : page.getTotal(),
                "generated_at", LocalDateTime.now().toString(),
                "scope", orderScope(number, status, begin, end),
                "source", "spring_internal_api"
        ), requestId);
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, Object> orderDetail(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @PathVariable Long orderId) {
        requireAdmin(actorType, roles);
        if (orderId == null || orderId <= 0) throw new IllegalArgumentException("orderId must be positive");
        return success(orderSummary(orderService.details(orderId, false)), requestId);
    }

    @GetMapping("/menu")
    public Map<String, Object> menu(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, name = "category_id") Long categoryId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        requireAdmin(actorType, roles);
        DishPageQueryDTO pageQuery = new DishPageQueryDTO();
        pageQuery.setPage(safePage(page));
        pageQuery.setPageSize(safeLimit(limit));
        pageQuery.setName(StringUtils.hasText(name) ? name : query);
        pageQuery.setStatus(status);
        pageQuery.setCategoryId(categoryId == null ? null : categoryId.intValue());
        PageResult pageResult = dishService.pageQuery(pageQuery);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object record : records(pageResult)) {
            if (record instanceof DishVO) {
                DishVO dish = (DishVO) record;
                items.add(mapOf(
                        "id", dish.getId(), "name", dish.getName(),
                        "category_id", dish.getCategoryId(), "category_name", dish.getCategoryName(),
                        "price", dish.getPrice(), "status", dish.getStatus(),
                        "description", dish.getDescription(), "updated_at",
                        dish.getUpdateTime() == null ? null : dish.getUpdateTime().toString()
                ));
            }
        }
        return catalogResult(items, pageResult == null ? 0 : pageResult.getTotal(),
                requestId, "single_store,name=" + value(pageQuery.getName())
                        + ",status=" + value(status) + ",category_id=" + value(categoryId)
                        + ",page=" + pageQuery.getPage() + ",page_size=" + pageQuery.getPageSize());
    }

    @GetMapping("/categories")
    public Map<String, Object> categories(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        requireAdmin(actorType, roles);
        CategoryPageQueryDTO pageQuery = new CategoryPageQueryDTO();
        pageQuery.setPage(safePage(page));
        pageQuery.setPageSize(safeLimit(limit));
        pageQuery.setName(StringUtils.hasText(name) ? name : query);
        pageQuery.setType(type);
        PageResult pageResult = categoryService.pageQuery(pageQuery);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object record : records(pageResult)) {
            if (record instanceof Category) {
                Category category = (Category) record;
                items.add(mapOf(
                        "id", category.getId(),
                        "name", category.getName(),
                        "type", category.getType(),
                        "status", category.getStatus(),
                        "sort", category.getSort()
                ));
            }
        }
        return success(mapOf("items", items, "total", pageResult == null ? 0 : pageResult.getTotal(),
                "generated_at", LocalDateTime.now().toString(),
                "scope", "single_store,name=" + value(pageQuery.getName())
                        + ",type=" + value(type) + ",status_ignored=" + value(status)
                        + ",page=" + pageQuery.getPage() + ",page_size=" + pageQuery.getPageSize(),
                "source", "spring_internal_api"), requestId);
    }

    @PostMapping("/categories")
    public Map<String, Object> createCategory(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestHeader("X-Confirmation-Token") String confirmationToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody InternalAgentAdminCategoryCreateDTO body) {
        requireConfirmedWrite(actorType, roles, confirmationToken, idempotencyKey, body);
        validateCategoryCreate(body);
        requireAuditReason(body.getAuditReason());
        String replay = beginIdempotentWrite(idempotencyKey);
        if (replay != null) return replay(requestId, replay);
        try {
            CategoryDTO category = new CategoryDTO();
            category.setName(body.getName().trim());
            category.setType(body.getType());
            category.setSort(body.getSort());
            categoryService.save(category);
            completeIdempotentWrite(idempotencyKey);
            Map<String, Object> created = mapOf(
                    "name", category.getName(), "type", category.getType(),
                    "sort", category.getSort(), "initial_status", 0);
            audit(requestId, "create_admin_category", category.getName(), null, created,
                    body.getAuditReason());
            return success(mapOf("status", "APPLIED", "category_name", category.getName(),
                    "new_value", created), requestId);
        } catch (RuntimeException ex) {
            failIdempotentWrite(idempotencyKey);
            throw ex;
        }
    }

    @GetMapping("/setmeals")
    public Map<String, Object> setmeals(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, name = "category_id") Long categoryId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        requireAdmin(actorType, roles);
        SetmealPageQueryDTO pageQuery = new SetmealPageQueryDTO();
        pageQuery.setPage(safePage(page));
        pageQuery.setPageSize(safeLimit(limit));
        pageQuery.setName(StringUtils.hasText(name) ? name : query);
        pageQuery.setStatus(status);
        pageQuery.setCategoryId(categoryId == null ? null : categoryId.intValue());
        PageResult pageResult = setmealService.pageQuery(pageQuery);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object record : records(pageResult)) {
            if (record instanceof SetmealVO) {
                SetmealVO setmeal = (SetmealVO) record;
                items.add(mapOf(
                        "id", setmeal.getId(), "name", setmeal.getName(),
                        "category_id", setmeal.getCategoryId(), "category_name", setmeal.getCategoryName(),
                        "price", setmeal.getPrice(), "status", setmeal.getStatus(),
                        "description", setmeal.getDescription(),
                        "updated_at", setmeal.getUpdateTime() == null
                                ? null : setmeal.getUpdateTime().toString()
                ));
            }
        }
        return catalogResult(items, pageResult == null ? 0 : pageResult.getTotal(),
                requestId, "single_store,name=" + value(pageQuery.getName())
                        + ",status=" + value(status) + ",category_id=" + value(categoryId)
                        + ",page=" + pageQuery.getPage() + ",page_size=" + pageQuery.getPageSize());
    }

    @GetMapping("/coupons")
    public Map<String, Object> coupons(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestParam(required = false, name = "query") String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "10") Integer limit) {
        requireAdmin(actorType, roles);
        Long requestedId = parsePositiveId(name);
        if (requestedId != null) {
            Coupon coupon = couponService.getById(requestedId);
            List<Map<String, Object>> exact = new ArrayList<>();
            if (coupon != null && (status == null || status.equals(coupon.getStatus()))) {
                exact.add(couponSummary(coupon));
            }
            return success(mapOf("items", exact, "total", exact.size(),
                    "generated_at", LocalDateTime.now().toString(),
                    "scope", "single_store,coupon_id=" + requestedId,
                    "source", "spring_internal_api"), requestId);
        }
        CouponPageQueryDTO query = new CouponPageQueryDTO();
        query.setPage(1);
        query.setPageSize(safeLimit(limit));
        query.setName(name);
        query.setStatus(status);
        PageResult page = couponService.pageQuery(query);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object record : records(page)) {
            if (record instanceof Coupon) {
                Coupon coupon = (Coupon) record;
                items.add(couponSummary(coupon));
            }
        }
        return success(mapOf("items", items, "total", page == null ? 0 : page.getTotal(),
                "generated_at", LocalDateTime.now().toString(),
                "scope", "single_store,query=" + value(name) + ",status=" + value(status),
                "source", "spring_internal_api"), requestId);
    }

    @GetMapping("/reviews")
    public Map<String, Object> reviews(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "10") Integer limit) {
        requireAdmin(actorType, roles);
        AdminReviewPageQueryDTO query = new AdminReviewPageQueryDTO();
        query.setPage(1);
        query.setPageSize(safeLimit(limit));
        query.setKeyword(keyword);
        query.setStatus(status);
        PageResult page = reviewService.pageForAdmin(query);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object record : records(page)) {
            if (record instanceof AdminReviewPageVO) {
                AdminReviewPageVO review = (AdminReviewPageVO) record;
                items.add(mapOf(
                        "id", review.getId(), "order_number", review.getOrderNumber(),
                        "dish_name", review.getDishName(),
                        "user_name_masked", maskName(review.getUserName()),
                        "rating", review.getRating(), "content_masked", maskContent(review.getContent()),
                        "status", review.getStatus(), "created_at", review.getCreateTime()
                ));
            }
        }
        return success(mapOf("items", items, "total", page == null ? 0 : page.getTotal(),
                "generated_at", LocalDateTime.now().toString(),
                "scope", "single_store,keyword=" + value(keyword) + ",status=" + value(status),
                "source", "spring_internal_api"), requestId);
    }

    @PostMapping("/shop/status")
    public Map<String, Object> setShopStatus(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestHeader("X-Confirmation-Token") String confirmationToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody InternalAgentAdminShopStatusDTO body) {
        requireConfirmedWrite(actorType, roles, confirmationToken, idempotencyKey, body);
        int expected = shopStatus(body.getExpectedStatus());
        int target = shopStatus(body.getStatus());
        requireAuditReason(body.getAuditReason());
        String replay = beginIdempotentWrite(idempotencyKey);
        if (replay != null) return replay(requestId, replay);
        try {
            if (!compareAndSetShopStatus(expected, target)) {
                throw new IllegalStateException("Shop status changed after preview");
            }
            completeIdempotentWrite(idempotencyKey);
            audit(requestId, "set_shop_status", SHOP_STATUS_KEY, expected, target, body.getAuditReason());
            return success(mapOf("status", "APPLIED", "old_value", shopStatus(expected),
                    "new_value", shopStatus(target)), requestId);
        } catch (RuntimeException ex) {
            failIdempotentWrite(idempotencyKey);
            throw ex;
        }
    }

    @PostMapping("/orders/{orderId}/actions")
    public Map<String, Object> updateOrder(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestHeader("X-Confirmation-Token") String confirmationToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable Long orderId,
            @RequestBody InternalAgentAdminOrderActionDTO body) {
        requireConfirmedWrite(actorType, roles, confirmationToken, idempotencyKey, body);
        if (orderId == null || orderId <= 0 || body.getExpectedStatus() == null) {
            throw new IllegalArgumentException("Valid order and expected status are required");
        }
        int target = orderTarget(body.getAction());
        requireOrderTransition(body.getExpectedStatus(), target);
        requireAuditReason(body.getAuditReason());
        String replay = beginIdempotentWrite(idempotencyKey);
        if (replay != null) return replay(requestId, replay);
        try {
            orderService.transitionByAgent(orderId, body.getExpectedStatus(), target);
            completeIdempotentWrite(idempotencyKey);
            audit(requestId, "update_order", orderId, body.getExpectedStatus(), target,
                    body.getAuditReason());
            return success(mapOf("status", "APPLIED", "order_id", orderId,
                    "old_value", body.getExpectedStatus(), "new_value", target), requestId);
        } catch (RuntimeException ex) {
            failIdempotentWrite(idempotencyKey);
            throw ex;
        }
    }

    @PostMapping("/coupons/{couponId}/actions")
    public Map<String, Object> manageCoupon(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestHeader("X-Confirmation-Token") String confirmationToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable Long couponId,
            @RequestBody InternalAgentAdminCouponActionDTO body) {
        requireConfirmedWrite(actorType, roles, confirmationToken, idempotencyKey, body);
        if (couponId == null || couponId <= 0 || body.getExpectedStatus() == null) {
            throw new IllegalArgumentException("Valid coupon and expected status are required");
        }
        int target = couponTarget(body.getAction());
        requireAuditReason(body.getAuditReason());
        String replay = beginIdempotentWrite(idempotencyKey);
        if (replay != null) return replay(requestId, replay);
        try {
            couponService.changeStatusByAgent(couponId, body.getExpectedStatus(), target);
            completeIdempotentWrite(idempotencyKey);
            audit(requestId, "manage_coupon", couponId, body.getExpectedStatus(), target,
                    body.getAuditReason());
            return success(mapOf("status", "APPLIED", "coupon_id", couponId,
                    "old_value", body.getExpectedStatus(), "new_value", target), requestId);
        } catch (RuntimeException ex) {
            failIdempotentWrite(idempotencyKey);
            throw ex;
        }
    }

    @PostMapping("/menu/items")
    public Map<String, Object> createDish(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestHeader("X-Confirmation-Token") String confirmationToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody InternalAgentAdminDishMutationDTO body) {
        requireConfirmedWrite(actorType, roles, confirmationToken, idempotencyKey, body);
        validateDishCreate(body);
        requireAuditReason(body.getAuditReason());
        String replay = beginIdempotentWrite(idempotencyKey);
        if (replay != null) return replay(requestId, replay);
        try {
            DishDTO dish = dishDto(body);
            dishService.saveWithFlavor(dish);
            completeIdempotentWrite(idempotencyKey);
            Map<String, Object> created = dishChangeSummary(body);
            audit(requestId, "create_admin_dish", dish.getId(), null, created,
                    body.getAuditReason());
            return success(mapOf("status", "APPLIED", "dish_id", dish.getId(),
                    "new_value", created), requestId);
        } catch (RuntimeException ex) {
            failIdempotentWrite(idempotencyKey);
            throw ex;
        }
    }

    @PostMapping("/menu/items/{dishId}/actions")
    public Map<String, Object> updateDish(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestHeader("X-Confirmation-Token") String confirmationToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable Long dishId,
            @RequestBody InternalAgentAdminDishMutationDTO body) {
        requireConfirmedWrite(actorType, roles, confirmationToken, idempotencyKey, body);
        validateDishUpdate(dishId, body);
        requireAuditReason(body.getAuditReason());
        String replay = beginIdempotentWrite(idempotencyKey);
        if (replay != null) return replay(requestId, replay);
        try {
            Map<String, Object> changed = dishChangeSummary(body);
            dishService.updateByAgent(dishId, dishDto(body), body.getExpectedUpdatedAt());
            completeIdempotentWrite(idempotencyKey);
            audit(requestId, "update_admin_dish", dishId, body.getExpectedUpdatedAt(), changed,
                    body.getAuditReason());
            return success(mapOf("status", "APPLIED", "dish_id", dishId,
                    "new_value", changed), requestId);
        } catch (RuntimeException ex) {
            failIdempotentWrite(idempotencyKey);
            throw ex;
        }
    }

    @PostMapping("/coupons")
    public Map<String, Object> createCoupon(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader("X-Actor-Type") String actorType,
            @RequestHeader("X-Actor-Roles") String roles,
            @RequestHeader("X-Confirmation-Token") String confirmationToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody InternalAgentAdminCouponCreateDTO body) {
        requireConfirmedWrite(actorType, roles, confirmationToken, idempotencyKey, body);
        requireAuditReason(body.getAuditReason());
        String replay = beginIdempotentWrite(idempotencyKey);
        if (replay != null) return replay(requestId, replay);
        try {
            CouponDTO coupon = new CouponDTO();
            coupon.setName(body.getName());
            coupon.setType(body.getType());
            coupon.setDiscountAmount(body.getDiscountAmount());
            coupon.setMinimumAmount(body.getMinimumAmount());
            coupon.setTotalCount(body.getTotalCount());
            coupon.setPerUserLimit(body.getPerUserLimit());
            coupon.setValidFrom(body.getValidFrom());
            coupon.setValidUntil(body.getValidUntil());
            coupon.setStatus(body.getStatus());
            coupon.setDescription(body.getDescription());
            couponService.save(coupon);
            completeIdempotentWrite(idempotencyKey);
            Map<String, Object> created = mapOf(
                    "name", body.getName(), "discount_amount", body.getDiscountAmount(),
                    "minimum_amount", body.getMinimumAmount(), "total_count", body.getTotalCount(),
                    "per_user_limit", body.getPerUserLimit(), "valid_from", body.getValidFrom(),
                    "valid_until", body.getValidUntil(), "status", body.getStatus());
            audit(requestId, "create_admin_coupon", body.getName(), null, created,
                    body.getAuditReason());
            return success(mapOf("status", "APPLIED", "coupon_name", body.getName(),
                    "new_value", created), requestId);
        } catch (RuntimeException ex) {
            failIdempotentWrite(idempotencyKey);
            throw ex;
        }
    }

    private void requireConfirmedWrite(String actorType, String roles, String confirmationToken,
                                       String idempotencyKey, Object body) {
        requireAdmin(actorType, roles);
        if (!agentServiceProperties.isInternalWritesEnabled()) {
            throw new SecurityException("Agent internal writes are disabled");
        }
        if (body == null || !StringUtils.hasText(confirmationToken)
                || confirmationToken.length() < 20 || !StringUtils.hasText(idempotencyKey)
                || idempotencyKey.length() < 20) {
            throw new SecurityException("Valid confirmation and idempotency keys are required");
        }
    }

    private void requireAuditReason(String reason) {
        if (!StringUtils.hasText(reason) || reason.trim().length() < 5 || reason.length() > 200) {
            throw new IllegalArgumentException("audit_reason must contain 5 to 200 characters");
        }
    }

    private void validateCategoryCreate(InternalAgentAdminCategoryCreateDTO body) {
        if (body == null || !StringUtils.hasText(body.getName()) || body.getName().length() > 64
                || body.getType() == null || (body.getType() != 1 && body.getType() != 2)
                || body.getSort() == null || body.getSort() < 0 || body.getSort() > 9999) {
            throw new IllegalArgumentException("Valid category name, type and sort are required");
        }
    }

    private void validateDishCreate(InternalAgentAdminDishMutationDTO body) {
        if (!StringUtils.hasText(body.getName()) || body.getName().length() > 64
                || body.getCategoryId() == null || body.getCategoryId() <= 0
                || body.getPrice() == null || body.getPrice().compareTo(BigDecimal.ZERO) <= 0
                || body.getStatus() == null || (body.getStatus() != 0 && body.getStatus() != 1)) {
            throw new IllegalArgumentException("Valid dish name, category, price and status are required");
        }
        requireEnabledDishCategory(body.getCategoryId());
        validateFlavors(body.getFlavors());
    }

    private void requireEnabledDishCategory(Long categoryId) {
        for (Category category : nullSafe(categoryService.list(1))) {
            if (categoryId.equals(category.getId()) && Integer.valueOf(1).equals(category.getStatus())) {
                return;
            }
        }
        throw new IllegalArgumentException("category_id must reference an enabled dish category");
    }

    private void validateDishUpdate(Long dishId, InternalAgentAdminDishMutationDTO body) {
        if (dishId == null || dishId <= 0 || body.getExpectedUpdatedAt() == null) {
            throw new IllegalArgumentException("Valid dish and expected_updated_at are required");
        }
        if (body.getFlavors() != null) {
            throw new IllegalArgumentException("Flavor replacement is not enabled for Agent updates");
        }
        if (body.getName() == null && body.getCategoryId() == null && body.getPrice() == null
                && body.getImage() == null && body.getDescription() == null
                && body.getStatus() == null) {
            throw new IllegalArgumentException("At least one dish field must be changed");
        }
        if (body.getName() != null && (!StringUtils.hasText(body.getName())
                || body.getName().length() > 64)) {
            throw new IllegalArgumentException("Invalid dish name");
        }
        if (body.getCategoryId() != null && body.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Invalid dish category");
        }
        if (body.getPrice() != null && body.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid dish price");
        }
        if (body.getStatus() != null && body.getStatus() != 0 && body.getStatus() != 1) {
            throw new IllegalArgumentException("Invalid dish status");
        }
    }

    private void validateFlavors(List<DishFlavor> flavors) {
        if (flavors == null) return;
        if (flavors.size() > 20) throw new IllegalArgumentException("Too many dish flavors");
        for (DishFlavor flavor : flavors) {
            if (flavor == null || !StringUtils.hasText(flavor.getName())
                    || flavor.getName().length() > 50 || !StringUtils.hasText(flavor.getValue())
                    || flavor.getValue().length() > 200) {
                throw new IllegalArgumentException("Invalid dish flavor");
            }
        }
    }

    private DishDTO dishDto(InternalAgentAdminDishMutationDTO body) {
        DishDTO dish = new DishDTO();
        dish.setName(body.getName());
        dish.setCategoryId(body.getCategoryId());
        dish.setPrice(body.getPrice());
        dish.setImage(body.getImage());
        dish.setDescription(body.getDescription());
        dish.setStatus(body.getStatus());
        if (body.getFlavors() != null) {
            List<DishFlavor> sanitized = new ArrayList<>();
            for (DishFlavor source : body.getFlavors()) {
                DishFlavor flavor = new DishFlavor();
                flavor.setName(source.getName());
                flavor.setValue(source.getValue());
                sanitized.add(flavor);
            }
            dish.setFlavors(sanitized);
        }
        return dish;
    }

    private Map<String, Object> dishChangeSummary(InternalAgentAdminDishMutationDTO body) {
        return mapOf("name", body.getName(), "category_id", body.getCategoryId(),
                "price", body.getPrice(), "image", body.getImage(),
                "description", body.getDescription(), "status", body.getStatus());
    }

    private int shopStatus(String status) {
        if ("OPEN".equals(status)) return 1;
        if ("CLOSED".equals(status)) return 0;
        throw new IllegalArgumentException("status must be OPEN or CLOSED");
    }

    private String shopStatus(int status) {
        return status == 1 ? "OPEN" : "CLOSED";
    }

    private int orderTarget(String action) {
        if ("confirm".equals(action)) return 3;
        if ("deliver".equals(action)) return 4;
        if ("complete".equals(action)) return 5;
        throw new IllegalArgumentException("Unsupported order action");
    }

    private void requireOrderTransition(int expected, int target) {
        if (!((expected == 2 && target == 3)
                || (expected == 3 && target == 4)
                || (expected == 4 && target == 5))) {
            throw new IllegalArgumentException("Order action does not match the expected status");
        }
    }

    private int couponTarget(String action) {
        if ("activate".equals(action)) return 1;
        if ("deactivate".equals(action)) return 0;
        throw new IllegalArgumentException("Unsupported coupon action");
    }

    private String beginIdempotentWrite(String idempotencyKey) {
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
        return "agent:admin-write:" + BaseContext.getCurrentId() + ":" + idempotencyKey;
    }

    private boolean compareAndSetShopStatus(final int expected, final int target) {
        List<Object> result = (List<Object>) redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) {
                operations.watch(SHOP_STATUS_KEY);
                Object current = operations.opsForValue().get(SHOP_STATUS_KEY);
                int currentStatus = current instanceof Number ? ((Number) current).intValue() : 0;
                if (currentStatus != expected) {
                    operations.unwatch();
                    return Collections.emptyList();
                }
                operations.multi();
                operations.opsForValue().set(SHOP_STATUS_KEY, target);
                return operations.exec();
            }
        });
        return result != null && !result.isEmpty();
    }

    private Map<String, Object> replay(String requestId, String status) {
        return success(mapOf("status", status), requestId);
    }

    private void audit(String requestId, String action, Object resourceId,
                       Object oldValue, Object newValue, String reason) {
        log.info("agent_admin_write request_id={} actor_id={} action={} resource_id={} " +
                        "old_value={} new_value={} reason={}",
                requestId, BaseContext.getCurrentId(), action, resourceId,
                oldValue, newValue, reason.replaceAll("[\\r\\n]", " "));
    }

    private Map<String, Object> orderSummary(OrderVO order) {
        return mapOf(
                "id", order.getId(), "number", order.getNumber(), "status", order.getStatus(),
                "amount", order.getAmount(), "order_time", order.getOrderTime(),
                "items_summary", order.getOrderDishes(),
                "generated_at", LocalDateTime.now().toString(),
                "scope", "single_store,order_id=" + order.getId(),
                "source", "spring_internal_api"
        );
    }

    private Map<String, Object> catalogResult(List<Map<String, Object>> items,
                                               long total,
                                               String requestId, String scope) {
        return success(mapOf("items", items, "total", total,
                "generated_at", LocalDateTime.now().toString(), "scope", scope,
                "source", "spring_internal_api"), requestId);
    }

    private void requireAdmin(String actorType, String roles) {
        if (!"admin".equals(actorType) || !StringUtils.hasText(roles) || !roles.contains("ADMIN")) {
            throw new SecurityException("Admin actor required");
        }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        return StringUtils.hasText(value) ? LocalDate.parse(value) : fallback;
    }

    private int safeLimit(Integer limit) {
        return Math.max(1, Math.min(limit == null ? 10 : limit, 20));
    }

    private int safePage(Integer page) {
        return Math.max(1, page == null ? 1 : page);
    }

    private boolean matches(String value, String query) {
        return !StringUtils.hasText(query) || (value != null && value.toLowerCase(Locale.ROOT)
                .contains(query.trim().toLowerCase(Locale.ROOT)));
    }

    private boolean matchesResource(Long id, String name, String query) {
        return !StringUtils.hasText(query)
                || (id != null && String.valueOf(id).equals(query.trim()))
                || matches(name, query);
    }

    private Long parsePositiveId(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<String, Object> couponSummary(Coupon coupon) {
        return mapOf(
                "id", coupon.getId(), "name", coupon.getName(),
                "discount_amount", coupon.getDiscountAmount(),
                "minimum_amount", coupon.getMinimumAmount(),
                "remaining_count", coupon.getRemainingCount(),
                "valid_from", coupon.getValidFrom(), "valid_until", coupon.getValidUntil(),
                "status", coupon.getStatus()
        );
    }

    private String maskName(String value) {
        return StringUtils.hasText(value) ? value.substring(0, 1) + "**" : "***";
    }

    private String maskContent(String value) {
        if (!StringUtils.hasText(value)) return "";
        return value.length() <= 80 ? value : value.substring(0, 80) + "…";
    }

    private String orderScope(String number, Integer status, String begin, String end) {
        return "number=" + value(number) + ",status=" + value(status)
                + ",begin=" + value(begin) + ",end=" + value(end);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
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
        for (int i = 0; i < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
