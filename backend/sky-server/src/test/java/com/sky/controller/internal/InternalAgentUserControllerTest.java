package com.sky.controller.internal;

import com.sky.context.BaseContext;
import com.sky.dto.InternalAgentCartChangeDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.service.AddressBookService;
import com.sky.service.CouponService;
import com.sky.service.DishService;
import com.sky.service.OrderService;
import com.sky.service.SensitiveWordService;
import com.sky.service.ShoppingCartService;
import com.sky.service.SetmealService;
import com.sky.vo.OrderVO;
import com.sky.vo.DishVO;
import com.sky.vo.SensitiveWordCheckVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InternalAgentUserControllerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private ShoppingCartService shoppingCartService;
    @Mock
    private AddressBookService addressBookService;
    @Mock
    private CouponService couponService;
    @Mock
    private SensitiveWordService sensitiveWordService;
    @Mock
    private RedisTemplate redisTemplate;
    @Mock
    private ValueOperations valueOperations;
    @Mock
    private DishService dishService;
    @Mock
    private SetmealService setmealService;

    private InternalAgentUserController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalAgentUserController(
                orderService,
                shoppingCartService,
                addressBookService,
                couponService,
                sensitiveWordService,
                redisTemplate,
                dishService,
                setmealService
        );
        BaseContext.setCurrentId(1001L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void orderDetailReturnsOnlyAgentSafeFields() {
        OrderVO order = new OrderVO();
        order.setId(10L);
        order.setNumber("ORDER-10");
        order.setStatus(5);
        order.setAmount(new BigDecimal("28.00"));
        order.setPhone("13812345678");
        order.setAddress("full private address");
        order.setUserId(1001L);
        when(orderService.details(10L, true)).thenReturn(order);

        Map<String, Object> response = controller.orderDetail("req-order", "user", 10L);

        Map<?, ?> data = (Map<?, ?>) response.get("data");
        assertEquals("ORDER-10", data.get("number"));
        assertFalse(data.containsKey("phone"));
        assertFalse(data.containsKey("address"));
        assertFalse(data.containsKey("user_id"));
    }

    @Test
    void addressesAreMaskedAndBoundToCurrentActor() {
        AddressBook address = AddressBook.builder()
                .id(20L)
                .userId(1001L)
                .consignee("Zhang San")
                .phone("13812345678")
                .provinceName("Shanghai")
                .cityName("Shanghai")
                .districtName("Pudong")
                .detail("No. 1 Example Road")
                .isDefault(1)
                .build();
        when(addressBookService.list(any(AddressBook.class))).thenReturn(singletonList(address));

        Map<String, Object> response = controller.addresses("req-address", "user", false);

        Map<?, ?> data = (Map<?, ?>) response.get("data");
        Map<?, ?> item = (Map<?, ?>) ((List<?>) data.get("items")).get(0);
        assertEquals("Z**", item.get("consignee_masked"));
        assertEquals("138****5678", item.get("phone_masked"));
        assertEquals("***", item.get("detail_masked"));
        assertFalse(item.containsKey("user_id"));
    }

    @Test
    void userPrivateEndpointsRejectAdminActor() {
        assertThrows(
                SecurityException.class,
                () -> controller.cart("req-admin", "admin")
        );
    }

    @Test
    void sensitiveWordCheckDoesNotExposeMatchedWordList() {
        SensitiveWordCheckVO scan = new SensitiveWordCheckVO();
        scan.setHit(true);
        scan.setContent("safe *** text");
        scan.setWords(singletonList("private-dictionary-entry"));
        when(sensitiveWordService.scanText("unsafe text")).thenReturn(scan);

        Map<String, Object> response = controller.checkSensitiveWords(
                "req-sensitive",
                "user",
                singletonMap("text", "unsafe text")
        );

        Map<?, ?> data = (Map<?, ?>) response.get("data");
        assertEquals(false, data.get("safe"));
        assertEquals("safe *** text", data.get("masked_text"));
        assertFalse(data.containsKey("words"));
    }

    @Test
    void reviewDraftRequiresOwnedCompletedOrderAndOrderedDish() {
        OrderDetail detail = new OrderDetail();
        detail.setDishId(9L);
        detail.setName("宫保鸡丁");
        OrderVO order = new OrderVO();
        order.setId(10L);
        order.setStatus(5);
        order.setOrderDetailList(singletonList(detail));
        when(orderService.details(10L, true)).thenReturn(order);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_id", 10L);
        body.put("dish_id", 9L);
        body.put("rating", 5);
        body.put("highlights", "味道很好");
        Map<String, Object> response = controller.checkReviewDraft(
                "req-review", "user", body
        );

        Map<?, ?> data = (Map<?, ?>) response.get("data");
        assertEquals(true, data.get("eligible"));
        assertEquals(true, data.get("safe"));
        assertEquals("宫保鸡丁", data.get("dish_name"));
    }

    @Test
    void reviewDraftRejectsIncompleteOrder() {
        OrderVO order = new OrderVO();
        order.setStatus(4);
        when(orderService.details(10L, true)).thenReturn(order);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_id", 10L);
        body.put("dish_id", 9L);
        assertThrows(IllegalStateException.class, () -> controller.checkReviewDraft(
                "req-review", "user", body
        ));
    }

    @Test
    void confirmedCartWriteExecutesOnce() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
        InternalAgentCartChangeDTO body = new InternalAgentCartChangeDTO();
        body.setAction("clear");

        Map<String, Object> response = controller.changeCart(
                "req-write",
                "user",
                "confirmation-token-1234567890",
                "idempotency-key-1234567890",
                body
        );

        assertEquals(true, response.get("ok"));
        verify(shoppingCartService).cleanShoppingCart();
    }

    @Test
    void replayedCartWriteDoesNotExecuteAgain() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(false);
        when(valueOperations.get(any())).thenReturn("COMPLETED");
        InternalAgentCartChangeDTO body = new InternalAgentCartChangeDTO();
        body.setAction("clear");

        controller.changeCart(
                "req-replay",
                "user",
                "confirmation-token-1234567890",
                "idempotency-key-1234567890",
                body
        );

        verify(shoppingCartService, never()).cleanShoppingCart();
    }

    @Test
    void cartWriteRejectsMissingConfirmationBeforeMutation() {
        InternalAgentCartChangeDTO body = new InternalAgentCartChangeDTO();
        body.setAction("clear");

        assertThrows(
                SecurityException.class,
                () -> controller.changeCart("req-invalid", "user", "short", "short", body)
        );

        verify(shoppingCartService, never()).cleanShoppingCart();
    }

    @Test
    void cartAddRejectsPriceChangeAfterConfirmation() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), anyLong(), any())).thenReturn(true);
        DishVO dish = new DishVO();
        dish.setId(9L);
        dish.setStatus(1);
        dish.setPrice(new BigDecimal("19.00"));
        when(dishService.getByIdWithFlavor(9L)).thenReturn(dish);
        InternalAgentCartChangeDTO body = new InternalAgentCartChangeDTO();
        body.setAction("add");
        body.setDishId(9L);
        body.setQuantity(1);
        body.setExpectedUnitAmount(new BigDecimal("18.00"));

        assertThrows(IllegalStateException.class, () -> controller.changeCart(
                "req-price", "user", "confirmation-token-1234567890",
                "idempotency-key-1234567890", body));

        verify(shoppingCartService, never()).addShoppingCart(any());
    }
}
