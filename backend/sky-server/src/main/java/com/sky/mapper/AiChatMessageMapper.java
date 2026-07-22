package com.sky.mapper;

import com.sky.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiChatMessageMapper {
    void insert(AiChatMessage message);

    List<AiChatMessage> listRecentBySessionId(@Param("sessionId") Long sessionId, @Param("limit") Integer limit);

    void deleteById(@Param("id") Long id);

    void deleteBySessionId(@Param("sessionId") Long sessionId);
}
