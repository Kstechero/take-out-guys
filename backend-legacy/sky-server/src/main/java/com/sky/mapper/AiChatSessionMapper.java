package com.sky.mapper;

import com.sky.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiChatSessionMapper {
    void insert(AiChatSession session);

    AiChatSession getByIdAndOwner(@Param("id") Long id, @Param("scope") String scope, @Param("ownerId") Long ownerId);

    List<AiChatSession> listByOwner(@Param("scope") String scope, @Param("ownerId") Long ownerId);

    void updateMeta(AiChatSession session);

    void deleteByIdAndOwner(@Param("id") Long id, @Param("scope") String scope, @Param("ownerId") Long ownerId);
}
