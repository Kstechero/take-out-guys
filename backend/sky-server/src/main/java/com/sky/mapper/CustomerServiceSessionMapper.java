package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.CustomerServiceSessionPageQueryDTO;
import com.sky.entity.CustomerServiceSession;
import com.sky.vo.CustomerServiceSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerServiceSessionMapper {
    void insert(CustomerServiceSession session);

    CustomerServiceSession getOpenByUserId(@Param("userId") Long userId);

    CustomerServiceSession getById(@Param("id") Long id);

    void updateMeta(CustomerServiceSession session);

    void closeById(@Param("id") Long id, @Param("closedTime") java.time.LocalDateTime closedTime);

    Page<CustomerServiceSessionVO> pageQuery(CustomerServiceSessionPageQueryDTO queryDTO);
}
