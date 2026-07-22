package com.sky.controller.internal;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/internal/agent")
public class InternalAgentController {

    private static final String SHOP_STATUS_KEY = "SHOP_STATUS";

    private final RedisTemplate redisTemplate;
    private final DishService dishService;
    private final SetmealService setmealService;

    public InternalAgentController(RedisTemplate redisTemplate,
                                   DishService dishService,
                                   SetmealService setmealService) {
        this.redisTemplate = redisTemplate;
        this.dishService = dishService;
        this.setmealService = setmealService;
    }

    @GetMapping("/shop/status")
    public Map<String, Object> shopStatus(@RequestHeader("X-Request-Id") String requestId) {
        Integer status = (Integer) redisTemplate.opsForValue().get(SHOP_STATUS_KEY);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", StatusConstant.ENABLE.equals(status) ? "OPEN" : "CLOSED");
        data.put("updated_at", LocalDateTime.now().toString());
        return success(data, requestId);
    }

    @GetMapping("/menu/search")
    public Map<String, Object> menuSearch(@RequestHeader("X-Request-Id") String requestId,
                                          @RequestParam(required = false) String query,
                                          @RequestParam(required = false, name = "budget_max") BigDecimal budgetMax,
                                          @RequestParam(required = false, name = "dietary_preferences") String dietaryPreferences,
                                          @RequestParam(defaultValue = "5") Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit == null ? 5 : limit, 10));
        List<Map<String, Object>> candidates = new ArrayList<>();

        Dish dishProbe = new Dish();
        dishProbe.setStatus(StatusConstant.ENABLE);
        for (DishVO dish : nullSafe(dishService.listWithFlavor(dishProbe))) {
            if (withinBudget(dish.getPrice(), budgetMax) && matchesPreferences(dish.getName(), dish.getDescription(), query, dietaryPreferences)) {
                candidates.add(menuItem("dish", dish.getId(), dish.getName(), dish.getPrice(), dish.getDescription(), dish.getImage()));
            }
        }

        Setmeal setmealProbe = new Setmeal();
        setmealProbe.setStatus(StatusConstant.ENABLE);
        for (Setmeal setmeal : nullSafe(setmealService.list(setmealProbe))) {
            if (withinBudget(setmeal.getPrice(), budgetMax) && matchesPreferences(setmeal.getName(), setmeal.getDescription(), query, dietaryPreferences)) {
                candidates.add(menuItem("setmeal", setmeal.getId(), setmeal.getName(), setmeal.getPrice(), setmeal.getDescription(), setmeal.getImage()));
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", candidates.size() <= safeLimit ? candidates : candidates.subList(0, safeLimit));
        data.put("total", candidates.size());
        data.put("query", query);
        return success(data, requestId);
    }

    private boolean withinBudget(BigDecimal price, BigDecimal budgetMax) {
        return budgetMax == null || price == null || price.compareTo(budgetMax) <= 0;
    }

    private boolean matchesPreferences(String name, String description, String query, String preferences) {
        String source = normalize(name) + " " + normalize(description);
        String normalizedQuery = normalize(query);
        String normalizedPreferences = normalize(preferences);
        boolean namedMatch = !StringUtils.hasText(normalizedQuery)
                || source.contains(normalizedQuery)
                || normalizedQuery.contains(normalize(name));
        boolean nonSpicy = !normalizedQuery.contains("不辣") && !normalizedPreferences.contains("不辣")
                || !source.contains("辣");
        // Recommendation-style natural language may not contain a dish name, so preferences still filter the full menu.
        boolean recommendationQuery = normalizedQuery.contains("推荐") || normalizedQuery.contains("吃什么")
                || normalizedQuery.contains("菜") || normalizedQuery.contains("套餐")
                || normalizedQuery.contains("午餐") || normalizedQuery.contains("晚餐")
                || normalizedQuery.contains("不辣") || normalizedQuery.contains("素食");
        return (namedMatch || recommendationQuery) && nonSpicy;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> menuItem(String type, Long id, String name, BigDecimal price,
                                         String description, String image) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("id", id);
        item.put("name", name);
        item.put("price", price);
        item.put("description", description);
        item.put("image", image);
        return item;
    }

    private <T> List<T> nullSafe(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }

    private Map<String, Object> success(Object data, String requestId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("data", data);
        response.put("error_code", null);
        response.put("message", "");
        response.put("request_id", requestId);
        return response;
    }
}
