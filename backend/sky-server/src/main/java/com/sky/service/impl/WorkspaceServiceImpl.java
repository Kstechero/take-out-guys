package com.sky.service.impl;
import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.*;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/** 工作台统计业务。 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {
    @Autowired private OrderMapper orderMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private DishMapper dishMapper;
    @Autowired private SetmealMapper setmealMapper;

    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        Map<String,Object> map=new HashMap<>(); map.put("begin",begin); map.put("end",end);
        int total=orderMapper.countByMap(map); map.put("status",Orders.COMPLETED);
        int valid=orderMapper.countByMap(map); Double turnover=orderMapper.sumByMap(map);
        turnover=turnover==null?0D:turnover;
        Map<String,Object> userMap=new HashMap<>(); userMap.put("begin",begin); userMap.put("end",end);
        return BusinessDataVO.builder().turnover(turnover).validOrderCount(valid)
                .orderCompletionRate(total==0?0D:(double)valid/total)
                .unitPrice(valid==0?0D:turnover/valid).newUsers(userMapper.countByMap(userMap)).build();
    }
    public OrderOverViewVO getOrderOverview(){
        return getOrderOverview(LocalDate.now().atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX));
    }
    public OrderOverViewVO getOrderOverview(LocalDateTime begin, LocalDateTime end){
        Map<String,Object> m=new HashMap<>(); m.put("begin",begin); m.put("end",end);
        m.put("status",Orders.TO_BE_CONFIRMED); int waiting=orderMapper.countByMap(m);
        m.put("status",Orders.CONFIRMED); int delivered=orderMapper.countByMap(m);
        m.put("status",Orders.COMPLETED); int completed=orderMapper.countByMap(m);
        m.put("status",Orders.CANCELLED); int cancelled=orderMapper.countByMap(m);
        m.remove("status"); int all=orderMapper.countByMap(m);
        return OrderOverViewVO.builder().waitingOrders(waiting).deliveredOrders(delivered)
                .completedOrders(completed).cancelledOrders(cancelled).allOrders(all).build();
    }
    public DishOverViewVO getDishOverview(){return DishOverViewVO.builder().sold(dishMapper.countByStatus(StatusConstant.ENABLE)).discontinued(dishMapper.countByStatus(StatusConstant.DISABLE)).build();}
    public SetmealOverViewVO getSetmealOverview(){return SetmealOverViewVO.builder().sold(setmealMapper.countByStatus(StatusConstant.ENABLE)).discontinued(setmealMapper.countByStatus(StatusConstant.DISABLE)).build();}
}
