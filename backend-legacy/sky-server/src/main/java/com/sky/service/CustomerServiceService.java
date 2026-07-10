package com.sky.service;

import com.sky.dto.CustomerServiceMessageDTO;
import com.sky.dto.CustomerServiceReplyDTO;
import com.sky.dto.CustomerServiceSessionCreateDTO;
import com.sky.dto.CustomerServiceSessionPageQueryDTO;
import com.sky.dto.ServiceSessionEndDTO;
import com.sky.result.PageResult;
import com.sky.vo.CustomerServiceMessageVO;
import com.sky.vo.CustomerServiceSessionVO;

import java.util.List;

public interface CustomerServiceService {
    CustomerServiceSessionVO createOrGetSession(CustomerServiceSessionCreateDTO dto);

    CustomerServiceSessionVO currentSession();

    void endUserSession(ServiceSessionEndDTO dto);

    void sendUserMessage(CustomerServiceMessageDTO dto);

    List<CustomerServiceMessageVO> listUserMessages(Long sessionId, Long lastMessageId);

    PageResult pageSessions(CustomerServiceSessionPageQueryDTO queryDTO);

    List<CustomerServiceMessageVO> listAdminMessages(Long sessionId, Long lastMessageId);

    void reply(CustomerServiceReplyDTO dto);

    void endAdminSession(ServiceSessionEndDTO dto);
}
