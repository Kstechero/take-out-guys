package com.sky.controller.admin;
import com.sky.result.Result; import com.sky.service.ReportService; import com.sky.vo.*;
import org.springframework.beans.factory.annotation.Autowired; import org.springframework.format.annotation.DateTimeFormat; import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse; import java.time.LocalDate;
@RestController @RequestMapping("/admin/report")
public class ReportController {
 @Autowired private ReportService service;
 @GetMapping("/turnoverStatistics") public Result<TurnoverReportVO> turnover(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end){return Result.success(service.turnover(begin,end));}
 @GetMapping("/userStatistics") public Result<UserReportVO> users(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end){return Result.success(service.users(begin,end));}
 @GetMapping("/ordersStatistics") public Result<OrderReportVO> orders(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end){return Result.success(service.orders(begin,end));}
 @GetMapping("/top10") public Result<SalesTop10ReportVO> top10(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin,@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end){return Result.success(service.top10(begin,end));}
 @GetMapping("/export") public void export(HttpServletResponse response){service.export(response);}
}
