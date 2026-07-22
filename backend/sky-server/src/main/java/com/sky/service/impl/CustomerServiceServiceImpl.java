package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.CustomerServiceMessageDTO;
import com.sky.dto.CustomerServiceReplyDTO;
import com.sky.dto.CustomerServiceSessionCreateDTO;
import com.sky.dto.CustomerServiceSessionPageQueryDTO;
import com.sky.dto.ServiceSessionEndDTO;
import com.sky.entity.CustomerServiceMessage;
import com.sky.entity.CustomerServiceSession;
import com.sky.exception.BaseException;
import com.sky.mapper.CustomerServiceMessageMapper;
import com.sky.mapper.CustomerServiceSessionMapper;
import com.sky.result.PageResult;
import com.sky.service.CustomerServiceService;
import com.sky.service.SensitiveWordService;
import com.sky.vo.CustomerServiceMessageVO;
import com.sky.vo.CustomerServiceSessionVO;
import com.sky.vo.SensitiveWordCheckVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerServiceServiceImpl implements CustomerServiceService {

    private final CustomerServiceSessionMapper sessionMapper;
    private final CustomerServiceMessageMapper messageMapper;
    private final SensitiveWordService sensitiveWordService;

    public CustomerServiceServiceImpl(CustomerServiceSessionMapper sessionMapper,
                                      CustomerServiceMessageMapper messageMapper,
                                      SensitiveWordService sensitiveWordService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.sensitiveWordService = sensitiveWordService;
    }

    @Override
    @Transactional
    public CustomerServiceSessionVO createOrGetSession(CustomerServiceSessionCreateDTO dto) {
        Long userId = BaseContext.getCurrentId();
        CustomerServiceSession current = sessionMapper.getOpenByUserId(userId);
        if (current == null) {
            LocalDateTime now = LocalDateTime.now();
            current = new CustomerServiceSession();
            current.setUserId(userId);
            current.setSource(StringUtils.hasText(dto == null ? null : dto.getSource()) ? dto.getSource().trim() : "miniapp");
            current.setStatus(CustomerServiceSession.OPEN);
            current.setCreateTime(now);
            current.setUpdateTime(now);
            sessionMapper.insert(current);
        }
        if (dto != null && StringUtils.hasText(dto.getInitialMessage())) {
            CustomerServiceMessageDTO messageDTO = new CustomerServiceMessageDTO();
            messageDTO.setSessionId(current.getId());
            messageDTO.setContent(dto.getInitialMessage());
            messageDTO.setMessageType("text");
            sendUserMessage(messageDTO);
        }
        return toVO(sessionMapper.getById(current.getId()));
    }

    @Override
    public CustomerServiceSessionVO currentSession() {
        CustomerServiceSession session = sessionMapper.getOpenByUserId(BaseContext.getCurrentId());
        return session == null ? null : toVO(session);
    }

    @Override
    public void endUserSession(ServiceSessionEndDTO dto) {
        CustomerServiceSession session = requireOwnedOpenSession(dto == null ? null : dto.getSessionId());
        sessionMapper.closeById(session.getId(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public void sendUserMessage(CustomerServiceMessageDTO dto) {
        CustomerServiceSession session = requireOwnedOpenSession(dto == null ? null : dto.getSessionId());
        String content = requireCleanContent(dto == null ? null : dto.getContent());
        insertMessage(session.getId(), "user", BaseContext.getCurrentId(), normalizeMessageType(dto == null ? null : dto.getMessageType()), content, 0);
    }

    @Override
    @Transactional
    public List<CustomerServiceMessageVO> listUserMessages(Long sessionId, Long lastMessageId) {
        CustomerServiceSession session = requireOwnedSession(sessionId);
        List<CustomerServiceMessageVO> messages = messageMapper.listBySessionId(session.getId(), lastMessageId);
        messageMapper.markAsReadBySessionAndSenderType(session.getId(), "admin");
        return messages;
    }

    @Override
    public PageResult pageSessions(CustomerServiceSessionPageQueryDTO queryDTO) {
        if (queryDTO == null || queryDTO.getPage() == null || queryDTO.getPageSize() == null
                || queryDTO.getPage() < 1 || queryDTO.getPageSize() < 1) {
            throw new BaseException("分页参数不正确");
        }
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<CustomerServiceSessionVO> page = sessionMapper.pageQuery(queryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    public List<CustomerServiceMessageVO> listAdminMessages(Long sessionId, Long lastMessageId) {
        requireSession(sessionId);
        List<CustomerServiceMessageVO> messages = messageMapper.listBySessionId(sessionId, lastMessageId);
        messageMapper.markAsReadBySessionAndSenderType(sessionId, "user");
        return messages;
    }

    @Override
    @Transactional
    public void reply(CustomerServiceReplyDTO dto) {
        CustomerServiceSession session = requireOpenSession(dto == null ? null : dto.getSessionId());
        String content = requireCleanContent(dto == null ? null : dto.getContent());
        insertMessage(session.getId(), "admin", BaseContext.getCurrentId(), normalizeMessageType(dto == null ? null : dto.getMessageType()), content, 0);
    }

    @Override
    public void endAdminSession(ServiceSessionEndDTO dto) {
        CustomerServiceSession session = requireSession(dto == null ? null : dto.getSessionId());
        if (!CustomerServiceSession.CLOSED.equals(session.getStatus())) {
            sessionMapper.closeById(session.getId(), LocalDateTime.now());
        }
    }

    private void insertMessage(Long sessionId, String senderType, Long senderId, String messageType, String content, int flagged) {
        LocalDateTime now = LocalDateTime.now();
        CustomerServiceMessage message = new CustomerServiceMessage();
        message.setSessionId(sessionId);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setFlagged(flagged);
        message.setReadStatus(CustomerServiceMessage.UNREAD);
        message.setCreateTime(now);
        messageMapper.insert(message);

        CustomerServiceSession session = new CustomerServiceSession();
        session.setId(sessionId);
        session.setLastMessage(content);
        session.setLastMessageTime(now);
        session.setUpdateTime(now);
        sessionMapper.updateMeta(session);
    }

    private String requireCleanContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BaseException("消息内容不能为空");
        }
        SensitiveWordCheckVO moderation = sensitiveWordService.scanText(content);
        if (Boolean.TRUE.equals(moderation.getHit())) {
            throw new BaseException("消息内容包含敏感词：" + String.join("、", moderation.getWords()));
        }
        return content.trim();
    }

    private String normalizeMessageType(String messageType) {
        return StringUtils.hasText(messageType) ? messageType.trim() : "text";
    }

    private CustomerServiceSession requireOwnedOpenSession(Long sessionId) {
        CustomerServiceSession session = requireOwnedSession(sessionId);
        if (!CustomerServiceSession.OPEN.equals(session.getStatus())) {
            throw new BaseException("客服会话已结束");
        }
        return session;
    }

    private CustomerServiceSession requireOwnedSession(Long sessionId) {
        CustomerServiceSession session = requireSession(sessionId);
        if (!BaseContext.getCurrentId().equals(session.getUserId())) {
            throw new BaseException("客服会话不存在");
        }
        return session;
    }

    private CustomerServiceSession requireOpenSession(Long sessionId) {
        CustomerServiceSession session = requireSession(sessionId);
        if (!CustomerServiceSession.OPEN.equals(session.getStatus())) {
            throw new BaseException("客服会话已结束");
        }
        return session;
    }

    private CustomerServiceSession requireSession(Long sessionId) {
        if (sessionId == null) {
            throw new BaseException("会话编号不能为空");
        }
        CustomerServiceSession session = sessionMapper.getById(sessionId);
        if (session == null) {
            throw new BaseException("客服会话不存在");
        }
        return session;
    }

    private CustomerServiceSessionVO toVO(CustomerServiceSession session) {
        if (session == null) {
            return null;
        }
        CustomerServiceSessionVO vo = new CustomerServiceSessionVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setSource(session.getSource());
        vo.setStatus(session.getStatus());
        vo.setLastMessage(session.getLastMessage());
        vo.setLastMessageTime(session.getLastMessageTime());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }
}
