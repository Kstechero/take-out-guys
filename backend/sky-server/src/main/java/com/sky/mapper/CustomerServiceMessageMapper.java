package com.sky.mapper;

import com.sky.entity.CustomerServiceMessage;
import com.sky.vo.CustomerServiceMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerServiceMessageMapper {
    void insert(CustomerServiceMessage message);

    List<CustomerServiceMessageVO> listBySessionId(@Param("sessionId") Long sessionId, @Param("lastMessageId") Long lastMessageId);

    void markAsReadBySessionAndSenderType(@Param("sessionId") Long sessionId, @Param("senderType") String senderType);
}
