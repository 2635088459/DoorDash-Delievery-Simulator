package com.shydelivery.doordashsimulator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration
 * 配置实时双向通信
 * 
 * Phase 2: 实时位置追踪
 * 
 * 功能:
 * - 配送员实时位置更新
 * - 客户实时查看配送进度
 * - 订单状态变更通知
 * 
 * WebSocket Endpoints:
 * - /ws - WebSocket连接端点
 * 
 * STOMP Topics:
 * - /topic/delivery/{deliveryId} - 配送进度订阅
 * - /topic/driver/{driverId} - 配送员位置订阅
 * - /app/location - 位置更新发送端点
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    /**
     * 配置消息代理
     * - /topic: 用于广播消息（一对多）
     * - /app: 用于客户端发送消息到服务器
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，用于向客户端广播消息
        config.enableSimpleBroker("/topic", "/queue");
        
        // 设置应用程序目的地前缀（客户端发送消息时使用）
        config.setApplicationDestinationPrefixes("/app");
    }
    
    /**
     * 注册 STOMP 端点
     * 客户端通过这个端点建立 WebSocket 连接
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // 允许跨域（生产环境需要限制）
                .withSockJS();  // 启用 SockJS 回退选项
    }
}
