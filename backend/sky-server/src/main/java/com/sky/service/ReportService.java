package com.sky.service;
import com.sky.vo.*; import javax.servlet.http.HttpServletResponse; import java.time.LocalDate;
public interface ReportService {
 TurnoverReportVO turnover(LocalDate begin,LocalDate end); UserReportVO users(LocalDate begin,LocalDate end);
 OrderReportVO orders(LocalDate begin,LocalDate end); SalesTop10ReportVO top10(LocalDate begin,LocalDate end);
 void export(HttpServletResponse response);
}
