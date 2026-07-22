package com.sky.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.context.BaseContext;
import com.sky.properties.AgentServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalAgentAuthInterceptorTest {

    private InternalAgentAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        BaseContext.removeCurrentId();
        AgentServiceProperties properties = new AgentServiceProperties();
        properties.setInternalAuthToken("test-token");
        interceptor = new InternalAgentAuthInterceptor(properties, new ObjectMapper());
    }

    @Test
    void acceptsValidServiceAndActorContext() throws Exception {
        MockHttpServletRequest request = validRequest();

        boolean accepted = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(accepted);
        assertEquals(1001L, BaseContext.getCurrentId());

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        assertNull(BaseContext.getCurrentId());
    }

    @Test
    void rejectsExpiredActorContext() throws Exception {
        MockHttpServletRequest request = validRequest();
        request.removeHeader("X-Actor-Expires-At");
        request.addHeader("X-Actor-Expires-At", Instant.now().minus(1, ChronoUnit.MINUTES).toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean accepted = interceptor.preHandle(request, response, new Object());

        assertFalse(accepted);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("FORBIDDEN"));
    }

    private MockHttpServletRequest validRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Agent-Service-Token", "test-token");
        request.addHeader("X-Request-Id", "req-1");
        request.addHeader("X-Actor-Type", "user");
        request.addHeader("X-Actor-Id", "1001");
        request.addHeader("X-Actor-Roles", "USER");
        request.addHeader("X-Actor-Expires-At", Instant.now().plus(1, ChronoUnit.MINUTES).toString());
        return request;
    }
}
