package com.sky.service.ai.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.controller.admin.CategoryController;
import com.sky.controller.admin.CouponController;
import com.sky.controller.admin.DishController;
import com.sky.controller.admin.EmployeeController;
import com.sky.controller.admin.OrderController;
import com.sky.controller.admin.ReportController;
import com.sky.controller.admin.SetmealController;
import com.sky.controller.admin.ShopController;
import com.sky.controller.admin.WorkspaceController;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.dto.CouponDTO;
import com.sky.dto.CouponPageQueryDTO;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ai.knowledge.OperationsKnowledgeService;
import com.sky.service.ai.mcp.OperationsResourceCatalogService;
import com.sky.vo.OrderVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AdminAiToolExecutor {

    private static final int MAX_QUERY_PAGE_SIZE = 100;
    private static final int FETCH_BATCH_SIZE = 500;
    private static final int MAX_ALL_QUERY_ITEMS = 10000;

    private final ObjectMapper objectMapper;
    private final ShopController shopController;
    private final OrderController orderController;
    private final CouponController couponController;
    private final DishController dishController;
    private final SetmealController setmealController;
    private final CategoryController categoryController;
    private final EmployeeController employeeController;
    private final WorkspaceController workspaceController;
    private final ReportController reportController;
    private final OperationsKnowledgeService knowledgeService;
    private final OperationsResourceCatalogService resourceCatalogService;

    public AdminAiToolExecutor(ObjectMapper objectMapper,
                               ShopController shopController,
                               OrderController orderController,
                               CouponController couponController,
                               DishController dishController,
                               SetmealController setmealController,
                               CategoryController categoryController,
                               EmployeeController employeeController,
                               WorkspaceController workspaceController,
                               ReportController reportController,
                               OperationsKnowledgeService knowledgeService,
                               OperationsResourceCatalogService resourceCatalogService) {
        this.objectMapper = objectMapper;
        this.shopController = shopController;
        this.orderController = orderController;
        this.couponController = couponController;
        this.dishController = dishController;
        this.setmealController = setmealController;
        this.categoryController = categoryController;
        this.employeeController = employeeController;
        this.workspaceController = workspaceController;
        this.reportController = reportController;
        this.knowledgeService = knowledgeService;
        this.resourceCatalogService = resourceCatalogService;
    }

    public String execute(String name, JsonNode args) throws Exception {
        switch (name) {
            case "get_shop_status":
                return json(getShopStatusPayload());
            case "set_shop_status":
                return json(setShopStatusPayload(args));
            case "query_orders":
                return json(queryOrdersPayload(args));
            case "get_order_statistics":
                return json(getOrderStatisticsPayload());
            case "update_order":
                return json(updateOrderPayload(args));
            case "query_coupons":
                return json(queryCouponsPayload(args));
            case "manage_coupon":
                return json(manageCouponPayload(args));
            case "query_employees":
                return json(queryEmployeesPayload(args));
            case "manage_employee":
                return json(manageEmployeePayload(args));
            case "change_my_password":
                return json(changeMyPasswordPayload(args));
            case "query_categories":
                return json(queryCategoriesPayload(args));
            case "manage_category":
                return json(manageCategoryPayload(args));
            case "query_dishes":
                return json(queryDishesPayload(args));
            case "manage_dish":
                return json(manageDishPayload(args));
            case "query_setmeals":
                return json(querySetmealsPayload(args));
            case "manage_setmeal":
                return json(manageSetmealPayload(args));
            case "get_business_overview":
                return json(getBusinessOverviewPayload());
            case "get_business_trend":
                return json(getBusinessTrendPayload(args));
            case "list_operational_documents":
                return json(knowledgeService.listSources());
            case "search_operational_knowledge":
                return json(searchOperationalKnowledgePayload(args));
            case "list_resource_catalog":
                return json(resourceCatalogService.listCatalog());
            case "read_resource_detail":
                return json(readResourceDetailPayload(args));
            case "describe_upload_capability":
                return json(describeUploadCapabilityPayload());
            default:
                throw new IllegalArgumentException("Unknown admin tool: " + name);
        }
    }

    private Result<Integer> getShopStatusPayload() {
        return shopController.getStatus();
    }

    private Object setShopStatusPayload(JsonNode args) {
        requireConfirmed(args);
        int targetStatus = parseOpenClosedStatus(textArg(args, "status"));
        return mapOf("operation", shopController.setStatus(targetStatus), "current", shopController.getStatus());
    }

    private Object queryOrdersPayload(JsonNode args) {
        Long orderId = parseLong(args, "order_id");
        if (orderId != null) {
            return orderController.details(orderId);
        }

        OrdersPageQueryDTO queryDTO = new OrdersPageQueryDTO();
        queryDTO.setNumber(textArg(args, "order_number"));
        queryDTO.setStatus(parseOrderStatus(textArg(args, "status")));

        if (booleanArg(args, false, "all")) {
            return fetchAllPages((page, pageSize) -> {
                queryDTO.setPage(page);
                queryDTO.setPageSize(pageSize);
                return orderController.conditionSearch(queryDTO);
            });
        }

        queryDTO.setPage(intArg(args, 1, "page"));
        queryDTO.setPageSize(clamp(intArg(args, 20, "page_size", "pageSize"), 1, MAX_QUERY_PAGE_SIZE));
        return orderController.conditionSearch(queryDTO);
    }

    private Object getOrderStatisticsPayload() {
        return orderController.statistics();
    }

    private Object updateOrderPayload(JsonNode args) throws Exception {
        requireConfirmed(args);
        String action = normalize(args.path("action").asText());
        OrderVO target = resolveRequiredOrder(args);

        if ("confirm".equals(action)) {
            OrdersConfirmDTO dto = new OrdersConfirmDTO();
            dto.setId(target.getId());
            return mapOf("operation", orderController.confirm(dto), "details", orderController.details(target.getId()));
        }
        if ("reject".equals(action)) {
            OrdersRejectionDTO dto = new OrdersRejectionDTO();
            dto.setId(target.getId());
            dto.setRejectionReason(defaultReason(textArg(args, "reason"), "Rejected by admin AI assistant"));
            return mapOf("operation", orderController.rejection(dto), "details", orderController.details(target.getId()));
        }
        if ("cancel".equals(action)) {
            OrdersCancelDTO dto = new OrdersCancelDTO();
            dto.setId(target.getId());
            dto.setCancelReason(defaultReason(textArg(args, "reason"), "Cancelled by admin AI assistant"));
            return mapOf("operation", orderController.cancel(dto), "details", orderController.details(target.getId()));
        }
        if ("deliver".equals(action)) {
            return mapOf("operation", orderController.delivery(target.getId()), "details", orderController.details(target.getId()));
        }
        if ("complete".equals(action)) {
            return mapOf("operation", orderController.complete(target.getId()), "details", orderController.details(target.getId()));
        }
        throw new IllegalArgumentException("action must be confirm, reject, cancel, deliver, or complete");
    }

    private Object queryCouponsPayload(JsonNode args) {
        CouponPageQueryDTO queryDTO = new CouponPageQueryDTO();
        queryDTO.setName(textArg(args, "name"));
        queryDTO.setStatus(parseEnabledStatus(textArg(args, "status")));

        if (booleanArg(args, false, "all")) {
            return fetchAllPages((page, pageSize) -> {
                queryDTO.setPage(page);
                queryDTO.setPageSize(pageSize);
                return couponController.page(queryDTO);
            });
        }

        queryDTO.setPage(intArg(args, 1, "page"));
        queryDTO.setPageSize(clamp(intArg(args, 20, "page_size", "pageSize"), 1, MAX_QUERY_PAGE_SIZE));
        return couponController.page(queryDTO);
    }

    private Object manageCouponPayload(JsonNode args) {
        requireConfirmed(args);
        String action = normalize(args.path("action").asText());

        if ("create".equals(action)) {
            CouponDTO couponDTO = buildCouponDTO(requiredObject(args, "payload"));
            return mapOf("operation", couponController.save(couponDTO), "query", queryCouponsPayload(args));
        }

        Long couponId = requiredLong(args, "coupon_id");
        if ("update".equals(action)) {
            CouponDTO couponDTO = buildCouponDTO(requiredObject(args, "payload"));
            return mapOf("operation", couponController.update(couponId, couponDTO), "query", queryCouponsPayload(args));
        }
        if ("delete".equals(action)) {
            return mapOf("operation", couponController.delete(couponId), "query", queryCouponsPayload(args));
        }
        throw new IllegalArgumentException("action must be create, update, or delete");
    }

    private Object queryEmployeesPayload(JsonNode args) {
        Long employeeId = parseLong(args, "employee_id");
        if (employeeId != null) {
            return employeeController.getById(employeeId);
        }

        EmployeePageQueryDTO queryDTO = new EmployeePageQueryDTO();
        queryDTO.setName(textArg(args, "name"));

        if (booleanArg(args, false, "all")) {
            return fetchAllPages((page, pageSize) -> {
                queryDTO.setPage(page);
                queryDTO.setPageSize(pageSize);
                return employeeController.page(queryDTO);
            });
        }

        queryDTO.setPage(intArg(args, 1, "page"));
        queryDTO.setPageSize(clamp(intArg(args, 20, "page_size", "pageSize"), 1, MAX_QUERY_PAGE_SIZE));
        return employeeController.page(queryDTO);
    }

    private Object manageEmployeePayload(JsonNode args) {
        requireConfirmed(args);
        String action = normalize(args.path("action").asText());

        if ("create".equals(action)) {
            EmployeeDTO employeeDTO = buildEmployeeDTO(requiredObject(args, "payload"));
            return mapOf("operation", employeeController.save(employeeDTO), "query", queryEmployeesPayload(args));
        }

        Long employeeId = requiredLong(args, "employee_id");
        if ("update".equals(action)) {
            EmployeeDTO employeeDTO = buildEmployeeDTO(requiredObject(args, "payload"));
            employeeDTO.setId(employeeId);
            return mapOf("operation", employeeController.update(employeeDTO), "details", employeeController.getById(employeeId));
        }
        if ("toggle_status".equals(action)) {
            int status = requiredEnabledStatus(args, "status");
            return mapOf("operation", employeeController.startOrStop(status, employeeId), "details", employeeController.getById(employeeId));
        }
        throw new IllegalArgumentException("action must be create, update, or toggle_status");
    }

    private Object changeMyPasswordPayload(JsonNode args) {
        requireConfirmed(args);
        PasswordEditDTO dto = new PasswordEditDTO();
        dto.setOldPassword(requiredText(args, "old_password", "oldPassword"));
        dto.setNewPassword(requiredText(args, "new_password", "newPassword"));
        return mapOf("operation", employeeController.editPassword(dto), "message", "Password updated for current admin");
    }

    private Object queryCategoriesPayload(JsonNode args) {
        Integer categoryType = parseCategoryType(textArg(args, "category_type", "type"));
        String name = textArg(args, "name");

        if (booleanArg(args, false, "all") && !StringUtils.hasText(name)) {
            if (categoryType == null) {
                List<Category> categories = new ArrayList<>();
                categories.addAll(dataOrEmpty(categoryController.list(1)));
                categories.addAll(dataOrEmpty(categoryController.list(2)));
                return Result.success(categories);
            }
            return categoryController.list(categoryType);
        }

        CategoryPageQueryDTO queryDTO = new CategoryPageQueryDTO();
        queryDTO.setType(categoryType);
        queryDTO.setName(name);

        if (booleanArg(args, false, "all")) {
            return fetchAllPages((page, pageSize) -> {
                queryDTO.setPage(page);
                queryDTO.setPageSize(pageSize);
                return categoryController.page(queryDTO);
            });
        }

        queryDTO.setPage(intArg(args, 1, "page"));
        queryDTO.setPageSize(clamp(intArg(args, 20, "page_size", "pageSize"), 1, MAX_QUERY_PAGE_SIZE));
        return categoryController.page(queryDTO);
    }

    private Object manageCategoryPayload(JsonNode args) {
        requireConfirmed(args);
        String action = normalize(args.path("action").asText());

        if ("create".equals(action)) {
            CategoryDTO dto = buildCategoryDTO(requiredObject(args, "payload"));
            return mapOf("operation", categoryController.save(dto), "query", queryCategoriesPayload(args));
        }

        Long categoryId = requiredLong(args, "category_id");
        if ("update".equals(action)) {
            CategoryDTO dto = buildCategoryDTO(requiredObject(args, "payload"));
            dto.setId(categoryId);
            return mapOf("operation", categoryController.update(dto), "query", queryCategoriesPayload(args));
        }
        if ("delete".equals(action)) {
            return mapOf("operation", categoryController.deleteById(categoryId), "query", queryCategoriesPayload(args));
        }
        if ("toggle_status".equals(action)) {
            int status = requiredEnabledStatus(args, "status");
            return mapOf("operation", categoryController.startOrStop(status, categoryId), "query", queryCategoriesPayload(args));
        }
        throw new IllegalArgumentException("action must be create, update, delete, or toggle_status");
    }

    private Object queryDishesPayload(JsonNode args) {
        Long dishId = parseLong(args, "dish_id");
        if (dishId != null) {
            return dishController.getById(dishId);
        }

        DishPageQueryDTO queryDTO = new DishPageQueryDTO();
        queryDTO.setName(textArg(args, "name"));
        queryDTO.setCategoryId(parseInteger(args, "category_id", "categoryId"));
        queryDTO.setStatus(parseEnabledStatus(textArg(args, "status")));

        if (booleanArg(args, false, "all")) {
            return fetchAllPages((page, pageSize) -> {
                queryDTO.setPage(page);
                queryDTO.setPageSize(pageSize);
                return dishController.page(queryDTO);
            });
        }

        queryDTO.setPage(intArg(args, 1, "page"));
        queryDTO.setPageSize(clamp(intArg(args, 20, "page_size", "pageSize"), 1, MAX_QUERY_PAGE_SIZE));
        return dishController.page(queryDTO);
    }

    private Object manageDishPayload(JsonNode args) {
        requireConfirmed(args);
        String action = normalize(args.path("action").asText());

        if ("create".equals(action)) {
            DishDTO dto = buildDishDTO(requiredObject(args, "payload"));
            return mapOf("operation", dishController.save(dto), "query", queryDishesPayload(args));
        }
        if ("update".equals(action)) {
            Long dishId = requiredLong(args, "dish_id");
            DishDTO dto = buildDishDTO(requiredObject(args, "payload"));
            dto.setId(dishId);
            return mapOf("operation", dishController.getByIdWithFlavor(dto), "details", dishController.getById(dishId));
        }
        if ("delete".equals(action)) {
            List<Long> ids = parseLongList(args, "dish_ids");
            if (ids.isEmpty()) {
                ids = Collections.singletonList(requiredLong(args, "dish_id"));
            }
            return mapOf("operation", dishController.delete(ids), "query", queryDishesPayload(args));
        }
        if ("toggle_status".equals(action)) {
            Long dishId = requiredLong(args, "dish_id");
            int status = requiredEnabledStatus(args, "status");
            return mapOf("operation", dishController.startOrStop(status, dishId), "details", dishController.getById(dishId));
        }
        throw new IllegalArgumentException("action must be create, update, delete, or toggle_status");
    }

    private Object querySetmealsPayload(JsonNode args) {
        Long setmealId = parseLong(args, "setmeal_id");
        if (setmealId != null) {
            return setmealController.getById(setmealId);
        }

        SetmealPageQueryDTO queryDTO = new SetmealPageQueryDTO();
        queryDTO.setName(textArg(args, "name"));
        queryDTO.setCategoryId(parseInteger(args, "category_id", "categoryId"));
        queryDTO.setStatus(parseEnabledStatus(textArg(args, "status")));

        if (booleanArg(args, false, "all")) {
            return fetchAllPages((page, pageSize) -> {
                queryDTO.setPage(page);
                queryDTO.setPageSize(pageSize);
                return setmealController.page(queryDTO);
            });
        }

        queryDTO.setPage(intArg(args, 1, "page"));
        queryDTO.setPageSize(clamp(intArg(args, 20, "page_size", "pageSize"), 1, MAX_QUERY_PAGE_SIZE));
        return setmealController.page(queryDTO);
    }

    private Object manageSetmealPayload(JsonNode args) {
        requireConfirmed(args);
        String action = normalize(args.path("action").asText());

        if ("create".equals(action)) {
            SetmealDTO dto = buildSetmealDTO(requiredObject(args, "payload"));
            return mapOf("operation", setmealController.save(dto), "query", querySetmealsPayload(args));
        }
        if ("update".equals(action)) {
            Long setmealId = requiredLong(args, "setmeal_id");
            SetmealDTO dto = buildSetmealDTO(requiredObject(args, "payload"));
            dto.setId(setmealId);
            return mapOf("operation", setmealController.update(dto), "details", setmealController.getById(setmealId));
        }
        if ("delete".equals(action)) {
            List<Long> ids = parseLongList(args, "setmeal_ids");
            if (ids.isEmpty()) {
                ids = Collections.singletonList(requiredLong(args, "setmeal_id"));
            }
            return mapOf("operation", setmealController.delete(ids), "query", querySetmealsPayload(args));
        }
        if ("toggle_status".equals(action)) {
            Long setmealId = requiredLong(args, "setmeal_id");
            int status = requiredEnabledStatus(args, "status");
            return mapOf("operation", setmealController.startOrStop(status, setmealId), "details", setmealController.getById(setmealId));
        }
        throw new IllegalArgumentException("action must be create, update, delete, or toggle_status");
    }

    private Object getBusinessOverviewPayload() {
        return mapOf(
                "businessData", workspaceController.businessData(),
                "orderOverview", workspaceController.orders(),
                "dishOverview", workspaceController.dishes(),
                "setmealOverview", workspaceController.setmeals()
        );
    }

    private Object getBusinessTrendPayload(JsonNode args) {
        DateRange range = resolveDateRange(args);
        return mapOf(
                "range", mapOf("begin", range.begin.toString(), "end", range.end.toString()),
                "turnover", reportController.turnover(range.begin, range.end),
                "orders", reportController.orders(range.begin, range.end),
                "users", reportController.users(range.begin, range.end),
                "top10", reportController.top10(range.begin, range.end)
        );
    }

    private Object describeUploadCapabilityPayload() {
        return mapOf(
                "supported", false,
                "reason", "Current admin AI chat only accepts structured JSON tool arguments and cannot transmit binary files or multipart form-data.",
                "controller", "CommonController",
                "endpoint", "/admin/common/upload",
                "suggestion", "If the user already has an uploaded image URL, it can be used in dish or setmeal create/update tools."
        );
    }

    private Object searchOperationalKnowledgePayload(JsonNode args) {
        return knowledgeService.search(requiredText(args, "query"), parseInteger(args, "top_k", "topK"));
    }

    private Object readResourceDetailPayload(JsonNode args) {
        return resourceCatalogService.readResource(requiredText(args, "uri"));
    }

    private Object fetchAllPages(PageFetcher fetcher) {
        long total = 0L;
        boolean truncated = false;
        List<Object> records = new ArrayList<>();
        int page = 1;

        while (records.size() < MAX_ALL_QUERY_ITEMS) {
            Result<PageResult> result = fetcher.fetch(page, FETCH_BATCH_SIZE);
            PageResult pageResult = result == null ? null : result.getData();
            if (pageResult == null) {
                break;
            }
            if (page == 1) {
                total = pageResult.getTotal();
            }
            List currentRecords = pageResult.getRecords();
            if (currentRecords == null || currentRecords.isEmpty()) {
                break;
            }
            int remaining = MAX_ALL_QUERY_ITEMS - records.size();
            if (currentRecords.size() > remaining) {
                records.addAll(currentRecords.subList(0, remaining));
                truncated = true;
                break;
            }
            records.addAll(currentRecords);
            if (records.size() >= total) {
                break;
            }
            page++;
        }

        if (total > records.size()) {
            truncated = true;
        }

        Result<PageResult> merged = Result.success(new PageResult(total, records));
        if (!truncated) {
            return merged;
        }
        return mapOf("result", merged, "truncated", true, "limit", MAX_ALL_QUERY_ITEMS);
    }

    private OrderVO resolveRequiredOrder(JsonNode args) {
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

        OrdersPageQueryDTO queryDTO = new OrdersPageQueryDTO();
        queryDTO.setNumber(orderNumber);
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);
        Result<PageResult> result = orderController.conditionSearch(queryDTO);
        PageResult pageResult = result == null ? null : result.getData();
        if (pageResult == null || pageResult.getRecords() == null || pageResult.getRecords().isEmpty()) {
            throw new IllegalArgumentException("Order not found");
        }
        return objectMapper.convertValue(pageResult.getRecords().get(0), OrderVO.class);
    }

    private DateRange resolveDateRange(JsonNode args) {
        LocalDate today = LocalDate.now();
        String rangeKey = normalize(args.path("range_key").asText("today"));
        if ("yesterday".equals(rangeKey)) {
            return new DateRange(today.minusDays(1), today.minusDays(1));
        }
        if ("last7d".equals(rangeKey)) {
            return new DateRange(today.minusDays(6), today);
        }
        if ("last30d".equals(rangeKey)) {
            return new DateRange(today.minusDays(29), today);
        }
        if ("custom".equals(rangeKey)) {
            LocalDate start = parseDate(requiredText(args, "start_date", "startDate"));
            LocalDate end = parseDate(requiredText(args, "end_date", "endDate"));
            return new DateRange(start, end);
        }
        return new DateRange(today, today);
    }

    private CouponDTO buildCouponDTO(JsonNode payload) {
        CouponDTO dto = new CouponDTO();
        dto.setName(requiredText(payload, "name"));
        dto.setType(requiredInteger(payload, "type"));
        dto.setDiscountAmount(requiredDecimal(payload, "discount_amount", "discountAmount"));
        dto.setMinimumAmount(requiredDecimal(payload, "minimum_amount", "minimumAmount"));
        dto.setTotalCount(requiredInteger(payload, "total_count", "totalCount"));
        dto.setPerUserLimit(requiredInteger(payload, "per_user_limit", "perUserLimit"));
        dto.setValidFrom(requiredDateTime(payload, "valid_from", "validFrom"));
        dto.setValidUntil(requiredDateTime(payload, "valid_until", "validUntil"));
        dto.setStatus(optionalEnabledStatus(payload, "status"));
        dto.setDescription(textArg(payload, "description"));
        return dto;
    }

    private EmployeeDTO buildEmployeeDTO(JsonNode payload) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setUsername(requiredText(payload, "username"));
        dto.setName(requiredText(payload, "name"));
        dto.setPhone(requiredText(payload, "phone"));
        dto.setSex(requiredText(payload, "sex"));
        dto.setIdNumber(requiredText(payload, "id_number", "idNumber"));
        return dto;
    }

    private CategoryDTO buildCategoryDTO(JsonNode payload) {
        CategoryDTO dto = new CategoryDTO();
        dto.setType(requiredCategoryType(payload, "type", "category_type", "categoryType"));
        dto.setName(requiredText(payload, "name"));
        dto.setSort(requiredInteger(payload, "sort"));
        return dto;
    }

    private DishDTO buildDishDTO(JsonNode payload) {
        DishDTO dto = new DishDTO();
        dto.setName(requiredText(payload, "name"));
        dto.setCategoryId(requiredLong(payload, "category_id", "categoryId"));
        dto.setPrice(requiredDecimal(payload, "price"));
        dto.setImage(textArg(payload, "image"));
        dto.setDescription(textArg(payload, "description"));
        dto.setStatus(optionalEnabledStatus(payload, "status"));
        dto.setFlavors(parseDishFlavors(payload.path("flavors")));
        return dto;
    }

    private SetmealDTO buildSetmealDTO(JsonNode payload) {
        SetmealDTO dto = new SetmealDTO();
        dto.setName(requiredText(payload, "name"));
        dto.setCategoryId(requiredLong(payload, "category_id", "categoryId"));
        dto.setPrice(requiredDecimal(payload, "price"));
        dto.setImage(textArg(payload, "image"));
        dto.setDescription(textArg(payload, "description"));
        dto.setStatus(optionalEnabledStatus(payload, "status"));
        dto.setSetmealDishes(parseSetmealDishes(payload.path("setmeal_dishes"), payload.path("setmealDishes")));
        return dto;
    }

    private List<DishFlavor> parseDishFlavors(JsonNode... candidates) {
        JsonNode array = firstArray(candidates);
        if (array == null) {
            return new ArrayList<>();
        }
        List<DishFlavor> flavors = new ArrayList<>();
        for (JsonNode item : array) {
            DishFlavor flavor = new DishFlavor();
            flavor.setName(requiredText(item, "name"));
            JsonNode valuesNode = firstPresent(item, "values", "value");
            if (valuesNode != null && valuesNode.isArray()) {
                List<String> values = new ArrayList<>();
                for (JsonNode value : valuesNode) {
                    if (value != null && !value.isNull()) {
                        values.add(value.asText());
                    }
                }
                flavor.setValue(writeJson(values));
            } else if (valuesNode != null && valuesNode.isTextual()) {
                flavor.setValue(valuesNode.asText());
            } else {
                throw new IllegalArgumentException("Each flavor requires value or values");
            }
            flavors.add(flavor);
        }
        return flavors;
    }

    private List<SetmealDish> parseSetmealDishes(JsonNode... candidates) {
        JsonNode array = firstArray(candidates);
        if (array == null) {
            return new ArrayList<>();
        }
        List<SetmealDish> dishes = new ArrayList<>();
        for (JsonNode item : array) {
            SetmealDish setmealDish = new SetmealDish();
            setmealDish.setDishId(requiredLong(item, "dish_id", "dishId"));
            setmealDish.setName(textArg(item, "name"));
            setmealDish.setPrice(decimalArg(item, "price"));
            setmealDish.setCopies(requiredInteger(item, "copies"));
            dishes.add(setmealDish);
        }
        return dishes;
    }

    private void requireConfirmed(JsonNode args) {
        if (!booleanArg(args, false, "confirmed")) {
            throw new IllegalArgumentException("This tool mutates backend data and requires confirmed=true after explicit user confirmation");
        }
    }

    private Integer parseOrderStatus(String status) {
        String normalized = normalize(status);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        switch (normalized) {
            case "pending_payment": return 1;
            case "pending_accept": return 2;
            case "accepted": return 3;
            case "delivering": return 4;
            case "completed": return 5;
            case "cancelled": return 6;
            default: return null;
        }
    }

    private Integer parseEnabledStatus(String status) {
        String normalized = normalize(status);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if ("enabled".equals(normalized) || "on".equals(normalized) || "open".equals(normalized) || "1".equals(normalized)) {
            return 1;
        }
        if ("disabled".equals(normalized) || "off".equals(normalized) || "closed".equals(normalized)
                || "close".equals(normalized) || "0".equals(normalized)) {
            return 0;
        }
        return null;
    }

    private Integer optionalEnabledStatus(JsonNode node, String... fields) {
        JsonNode target = firstPresent(node, fields);
        if (target == null || target.isNull()) {
            return null;
        }
        if (target.isInt() || target.isLong()) {
            return target.asInt();
        }
        return parseEnabledStatus(target.asText());
    }

    private Integer requiredEnabledStatus(JsonNode node, String... fields) {
        Integer status = optionalEnabledStatus(node, fields);
        if (status == null) {
            throw new IllegalArgumentException("status must be enabled or disabled");
        }
        return status;
    }

    private Integer parseCategoryType(String categoryType) {
        String normalized = normalize(categoryType);
        if (!StringUtils.hasText(normalized) || "all".equals(normalized)) {
            return null;
        }
        if ("dish".equals(normalized) || "1".equals(normalized)) {
            return 1;
        }
        if ("setmeal".equals(normalized) || "2".equals(normalized)) {
            return 2;
        }
        return null;
    }

    private Integer requiredCategoryType(JsonNode node, String... fields) {
        Integer value = null;
        JsonNode target = firstPresent(node, fields);
        if (target != null && !target.isNull()) {
            value = target.isInt() || target.isLong() ? target.asInt() : parseCategoryType(target.asText());
        }
        if (value == null) {
            throw new IllegalArgumentException("category type must be dish or setmeal");
        }
        return value;
    }

    private int parseOpenClosedStatus(String status) {
        String normalized = normalize(status);
        if ("open".equals(normalized) || "1".equals(normalized)) {
            return 1;
        }
        if ("closed".equals(normalized) || "close".equals(normalized) || "0".equals(normalized)) {
            return 0;
        }
        throw new IllegalArgumentException("status must be open or closed");
    }

    private String defaultReason(String reason, String fallback) {
        return StringUtils.hasText(reason) ? reason : fallback;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
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

    private JsonNode firstArray(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            if (candidate != null && candidate.isArray()) {
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

    private Integer requiredInteger(JsonNode node, String... fields) {
        Integer value = parseInteger(node, fields);
        if (value == null) {
            throw new IllegalArgumentException(fields[0] + " is required");
        }
        return value;
    }

    private BigDecimal decimalArg(JsonNode node, String... fields) {
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
            throw new IllegalArgumentException(fields[0] + " must be a valid decimal");
        }
    }

    private BigDecimal requiredDecimal(JsonNode node, String... fields) {
        BigDecimal value = decimalArg(node, fields);
        if (value == null) {
            throw new IllegalArgumentException(fields[0] + " is required");
        }
        return value;
    }

    private LocalDate parseDate(String value) {
        return StringUtils.hasText(value) ? LocalDate.parse(value.trim()) : null;
    }

    private LocalDateTime requiredDateTime(JsonNode node, String... fields) {
        String value = requiredText(node, fields);
        try {
            if (value.contains("T")) {
                return LocalDateTime.parse(value);
            }
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fields[0] + " must be ISO datetime like 2026-07-05T10:00:00");
        }
    }

    private JsonNode requiredObject(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isObject()) {
            throw new IllegalArgumentException(field + " must be an object");
        }
        return value;
    }

    private List<Long> parseLongList(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull() || !value.isArray()) {
            return new ArrayList<>();
        }
        List<Long> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (item != null && !item.isNull()) {
                if (item.isNumber()) {
                    result.add(item.asLong());
                } else if (StringUtils.hasText(item.asText())) {
                    result.add(Long.valueOf(item.asText().trim()));
                }
            }
        }
        return result;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize nested payload");
        }
    }

    private <T> List<T> dataOrEmpty(Result<List<T>> result) {
        return result == null || result.getData() == null ? Collections.emptyList() : result.getData();
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    @FunctionalInterface
    private interface PageFetcher {
        Result<PageResult> fetch(int page, int pageSize);
    }

    private static class DateRange {
        private final LocalDate begin;
        private final LocalDate end;

        private DateRange(LocalDate begin, LocalDate end) {
            this.begin = begin;
            this.end = end;
        }
    }
}
