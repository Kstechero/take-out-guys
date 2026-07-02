package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务端，管理后台通过 {@code /ws/{sid}} 建立连接。
 */
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    /** 使用线程安全容器保存客户端会话。 */
    private static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        Session oldSession = SESSION_MAP.put(sid, session);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
            } catch (IOException exception) {
                log.warn("关闭旧 WebSocket 会话失败：{}", sid, exception);
            }
        }
        log.info("WebSocket 客户端 {} 已连接，当前连接数：{}", sid, SESSION_MAP.size());
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到 WebSocket 客户端 {} 的消息：{}", sid, message);
    }

    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        SESSION_MAP.remove(sid);
        log.info("WebSocket 客户端 {} 已断开，当前连接数：{}", sid, SESSION_MAP.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("sid") String sid) {
        SESSION_MAP.remove(sid, session);
        log.warn("WebSocket 客户端 {} 通信异常", sid, throwable);
    }

    /** 向所有已连接的管理端客户端推送消息。 */
    public void sendToAllClient(String message) {
        SESSION_MAP.forEach((sid, session) -> {
            if (!session.isOpen()) {
                SESSION_MAP.remove(sid, session);
                return;
            }
            session.getAsyncRemote().sendText(message, result -> {
                if (!result.isOK()) {
                    log.warn("向 WebSocket 客户端 {} 推送消息失败", sid, result.getException());
                }
            });
        });
    }
}
