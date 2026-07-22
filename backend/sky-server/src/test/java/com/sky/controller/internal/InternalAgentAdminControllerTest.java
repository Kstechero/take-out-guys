package com.sky.controller.internal;

import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.dto.InternalAgentAdminCategoryCreateDTO;
import com.sky.dto.InternalAgentAdminCouponActionDTO;
import com.sky.dto.InternalAgentAdminCouponCreateDTO;
import com.sky.dto.InternalAgentAdminDishMutationDTO;
import com.sky.dto.InternalAgentAdminOrderActionDTO;
import com.sky.dto.InternalAgentAdminShopStatusDTO;
import com.sky.entity.Category;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import com.sky.properties.AgentServiceProperties;
import com.sky.service.CategoryService;
import com.sky.service.CouponService;
import com.sky.service.DishService;
import com.sky.service.OrderService;
import com.sky.service.ReviewService;
import com.sky.service.SetmealService;
import com.sky.service.WorkspaceService;
import com.sky.vo.OrderVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalAgentAdminControllerTest {

    @Mock private OrderService orderService;
    @Mock private CategoryService categoryService;
    @Mock private DishService dishService;
    @Mock private SetmealService setmealService;
    @Mock private CouponService couponService;
    @Mock private ReviewService reviewService;
    @Mock private WorkspaceService workspaceService;
    @Mock private RedisTemplate redisTemplate;
    @Mock private RedisOperations redisOperations;
    @Mock private ValueOperations valueOperations;

    private AgentServiceProperties properties;
    private InternalAgentAdminController controller;

    @BeforeEach
    void setUp() {
        properties = new AgentServiceProperties();
        properties.setInternalWritesEnabled(true);
        controller = new InternalAgentAdminController(
                orderService, categoryService, dishService, setmealService, couponService,
                reviewService, workspaceService, redisTemplate, properties);
        BaseContext.setCurrentId(9L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void orderDetailReturnsNoUserContactOrAddress() {
        OrderVO order = new OrderVO();
        order.setId(10L);
        order.setNumber("ORDER-10");
        order.setStatus(5);
        order.setPhone("13812345678");
        order.setAddress("private address");
        order.setUserId(1001L);
        when(orderService.details(10L, false)).thenReturn(order);

        Map<String, Object> response = controller.orderDetail("req-admin", "admin", "ADMIN", 10L);
        Map<?, ?> data = (Map<?, ?>) response.get("data");

        assertEquals("ORDER-10", data.get("number"));
        assertFalse(data.containsKey("phone"));
        assertFalse(data.containsKey("address"));
        assertFalse(data.containsKey("user_id"));
    }

    @Test
    void orderSearchRejectsUnboundedQuery() {
        assertThrows(
                IllegalArgumentException.class,
                () -> controller.orders("req-admin", "admin", "ADMIN", null, null, null, null, 10)
        );
    }

    @Test
    void adminEndpointsRejectUserActor() {
        assertThrows(
                SecurityException.class,
                () -> controller.orderDetail("req-user", "user", "USER", 10L)
        );
    }

    @Test
    void menuSearchFiltersByCategoryIdAndKeepsMatchedTotal() {
        when(dishService.pageQuery(any())).thenReturn(new PageResult(3, Arrays.asList(
                dish(49L, "Rice", 12L),
                dish(50L, "Noodles", 12L)
        )));

        Map<String, Object> response = controller.menu(
                "req-menu", "admin", "ADMIN", null, null, 1, 12L, 1, 2);
        Map<?, ?> data = (Map<?, ?>) response.get("data");

        assertEquals(3, data.get("total"));
        assertEquals(2, ((java.util.List<?>) data.get("items")).size());
        org.mockito.ArgumentCaptor<DishPageQueryDTO> captor = forClass(DishPageQueryDTO.class);
        verify(dishService).pageQuery(captor.capture());
        assertEquals(12, captor.getValue().getCategoryId());
        assertEquals(2, captor.getValue().getPageSize());
    }

    @Test
    void categorySearchUsesManagementPageQuery() {
        when(categoryService.pageQuery(any())).thenReturn(new PageResult(1, Collections.singletonList(
                category(12L, 1)
        )));

        Map<String, Object> response = controller.categories(
                "req-category", "admin", "ADMIN", "Traditional", null, 1, null, 2, 20);
        Map<?, ?> data = (Map<?, ?>) response.get("data");

        assertEquals(1, data.get("total"));
        org.mockito.ArgumentCaptor<CategoryPageQueryDTO> captor = forClass(CategoryPageQueryDTO.class);
        verify(categoryService).pageQuery(captor.capture());
        assertEquals("Traditional", captor.getValue().getName());
        assertEquals(1, captor.getValue().getType());
        assertEquals(2, captor.getValue().getPage());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    void confirmedOrderTransitionUsesReviewedExpectedStatusOnce() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
        InternalAgentAdminOrderActionDTO body = new InternalAgentAdminOrderActionDTO();
        body.setAction("confirm");
        body.setExpectedStatus(2);
        body.setAuditReason("manual confirmed order transition");

        Map<String, Object> response = controller.updateOrder(
                "req-write", "admin", "ADMIN", "confirmation-token-1234567890",
                "idempotency-key-1234567890", 10L, body);

        assertEquals(true, response.get("ok"));
        verify(orderService).transitionByAgent(10L, 2, 3);
    }

    @Test
    void replayedOrderTransitionDoesNotExecuteAgain() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(false);
        when(valueOperations.get(any())).thenReturn("COMPLETED");
        InternalAgentAdminOrderActionDTO body = new InternalAgentAdminOrderActionDTO();
        body.setAction("deliver");
        body.setExpectedStatus(3);
        body.setAuditReason("manual delivery transition");

        controller.updateOrder(
                "req-replay", "admin", "ADMIN", "confirmation-token-1234567890",
                "idempotency-key-1234567890", 10L, body);

        verify(orderService, never()).transitionByAgent(any(), any(), any());
    }

    @Test
    void adminWriteIsRejectedWhenGraySwitchIsOff() {
        properties.setInternalWritesEnabled(false);
        InternalAgentAdminOrderActionDTO body = new InternalAgentAdminOrderActionDTO();
        body.setAction("confirm");
        body.setExpectedStatus(2);
        body.setAuditReason("manual confirmed order transition");

        assertThrows(SecurityException.class, () -> controller.updateOrder(
                "req-disabled", "admin", "ADMIN", "confirmation-token-1234567890",
                "idempotency-key-1234567890", 10L, body));
        verify(orderService, never()).transitionByAgent(any(), any(), any());
    }

    @Test
    void confirmedCouponTransitionUsesExpectedStatus() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
        InternalAgentAdminCouponActionDTO body = new InternalAgentAdminCouponActionDTO();
        body.setAction("activate");
        body.setExpectedStatus(0);
        body.setAuditReason("activate coupon after audit");

        controller.manageCoupon(
                "req-coupon", "admin", "ADMIN", "confirmation-token-1234567890",
                "idempotency-key-1234567890", 20L, body);

        verify(couponService).changeStatusByAgent(20L, 0, 1);
    }

    @Test
    void confirmedShopTransitionUsesRedisOptimisticLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation ->
                ((SessionCallback<?>) invocation.getArgument(0)).execute(redisOperations));
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("SHOP_STATUS")).thenReturn(1);
        when(redisOperations.exec()).thenReturn(Collections.singletonList(true));
        InternalAgentAdminShopStatusDTO body = new InternalAgentAdminShopStatusDTO();
        body.setExpectedStatus("OPEN");
        body.setStatus("CLOSED");
        body.setAuditReason("temporary maintenance closure");

        controller.setShopStatus(
                "req-shop", "admin", "ADMIN", "confirmation-token-1234567890",
                "idempotency-key-1234567890", body);

        verify(redisOperations).watch("SHOP_STATUS");
        verify(valueOperations).set("SHOP_STATUS", 0);
    }

    @Test
    void confirmedDishCreationCallsBusinessServiceOnce() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
        when(categoryService.list(1)).thenReturn(Collections.singletonList(category(11L, 1)));
        InternalAgentAdminDishMutationDTO body = new InternalAgentAdminDishMutationDTO();
        body.setName("Agent test dish");
        body.setCategoryId(11L);
        body.setPrice(new BigDecimal("18.00"));
        body.setStatus(0);
        body.setAuditReason("Agent test dish create audit reason");

        Map<String, Object> response = controller.createDish(
                "req-create-dish", "admin", "ADMIN", "confirmation-token-1234567890",
                "idempotency-key-create-dish", body);

        assertEquals(true, response.get("ok"));
        verify(dishService).saveWithFlavor(any());
    }

    @Test
    void confirmedDishCreationRejectsUnknownCategory() {
        when(categoryService.list(1)).thenReturn(Collections.emptyList());
        InternalAgentAdminDishMutationDTO body = new InternalAgentAdminDishMutationDTO();
        body.setName("Agent test dish");
        body.setCategoryId(1L);
        body.setPrice(new BigDecimal("18.00"));
        body.setStatus(0);
        body.setAuditReason("Agent test dish create audit reason");

        assertThrows(IllegalArgumentException.class, () -> controller.createDish(
                "req-create-dish", "admin", "ADMIN", "confirmation-token-1234567890",
                "idempotency-key-create-dish", body));
        verify(dishService, never()).saveWithFlavor(any());
    }

    @Test
    void confirmedCategoryCreationCallsBusinessService() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
        InternalAgentAdminCategoryCreateDTO body = new InternalAgentAdminCategoryCreateDTO();
        body.setName("Agent test category");
        body.setType(1);
        body.setSort(50);
        body.setAuditReason("Agent test category create audit reason");

        controller.createCategory(
                "req-create-category", "admin", "ADMIN", "confirmation-token-1234567890",
                "idempotency-key-create-category", body);

        org.mockito.ArgumentCaptor<CategoryDTO> captor = forClass(CategoryDTO.class);
        verify(categoryService).save(captor.capture());
        assertEquals("Agent test category", captor.getValue().getName());
        assertEquals(1, captor.getValue().getType());
        assertEquals(50, captor.getValue().getSort());
    }

    @Test
    void confirmedDishUpdatePassesReviewedVersion() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
        LocalDateTime expected = LocalDateTime.of(2026, 7, 22, 12, 0);
        InternalAgentAdminDishMutationDTO body = new InternalAgentAdminDishMutationDTO();
        body.setPrice(new BigDecimal("7.00"));
        body.setExpectedUpdatedAt(expected);
        body.setAuditReason("ingredient cost adjustment");

        controller.updateDish(
                "req-update-dish", "admin", "ADMIN", "confirmation-token-1234567890",
                "idempotency-key-update-dish", 46L, body);

        verify(dishService).updateByAgent(anyLong(), any(), any());
    }

    @Test
    void confirmedCouponCreationCallsBusinessService() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
        InternalAgentAdminCouponCreateDTO body = new InternalAgentAdminCouponCreateDTO();
        body.setName("Agent test coupon");
        body.setType(1);
        body.setDiscountAmount(new BigDecimal("5.00"));
        body.setMinimumAmount(new BigDecimal("30.00"));
        body.setTotalCount(100);
        body.setPerUserLimit(1);
        body.setValidFrom(LocalDateTime.of(2026, 7, 22, 0, 0));
        body.setValidUntil(LocalDateTime.of(2026, 8, 22, 0, 0));
        body.setStatus(0);
        body.setAuditReason("summer campaign coupon creation");

        controller.createCoupon(
                "req-create-coupon", "admin", "ADMIN", "confirmation-token-1234567890",
                "idempotency-key-create-coupon", body);

        verify(couponService).save(any());
    }

    private Category category(Long id, Integer status) {
        Category category = new Category();
        category.setId(id);
        category.setType(1);
        category.setName("Test category");
        category.setStatus(status);
        category.setSort(10);
        return category;
    }

    private DishVO dish(Long id, String name, Long categoryId) {
        DishVO dish = new DishVO();
        dish.setId(id);
        dish.setName(name);
        dish.setCategoryId(categoryId);
        dish.setPrice(new BigDecimal("2.00"));
        dish.setStatus(1);
        return dish;
    }
}
