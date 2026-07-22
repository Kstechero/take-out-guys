package com.sky.controller.admin;
import com.sky.result.Result; import com.sky.service.WorkspaceService; import com.sky.vo.*;
import org.springframework.beans.factory.annotation.Autowired; import org.springframework.web.bind.annotation.*;
import java.time.*;
/** 管理端工作台接口，响应数据可直接交给 ECharts 展示。 */
@RestController @RequestMapping("/admin/workspace")
public class WorkspaceController {
 @Autowired private WorkspaceService service;
 @GetMapping("/businessData") public Result<BusinessDataVO> businessData(){return Result.success(service.getBusinessData(LocalDate.now().atStartOfDay(),LocalDate.now().atTime(LocalTime.MAX)));}
 @GetMapping("/overviewOrders") public Result<OrderOverViewVO> orders(){return Result.success(service.getOrderOverview());}
 @GetMapping("/overviewDishes") public Result<DishOverViewVO> dishes(){return Result.success(service.getDishOverview());}
 @GetMapping("/overviewSetmeals") public Result<SetmealOverViewVO> setmeals(){return Result.success(service.getSetmealOverview());}
}
