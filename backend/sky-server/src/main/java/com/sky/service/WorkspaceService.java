package com.sky.service;
import com.sky.vo.*;
import java.time.LocalDateTime;
public interface WorkspaceService {
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);
    OrderOverViewVO getOrderOverview();
    DishOverViewVO getDishOverview();
    SetmealOverViewVO getSetmealOverview();
}
