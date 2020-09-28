package com.example.websocketbugdemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.util.UrlPathHelper;

import java.util.List;
import java.util.Objects;

import static org.springframework.messaging.simp.stomp.StompCommand.CONNECT;
import static org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE;
import static org.springframework.messaging.support.NativeMessageHeaderAccessor.NATIVE_HEADERS;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableStompBrokerRelay("/queue")
                .setVirtualHost("/")
                .setSystemHeartbeatSendInterval(30_000)
                .setSystemHeartbeatReceiveInterval(30_000);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("<<Registering Stomp Endpoints>>>");
        registry.setUrlPathHelper(new UrlPathHelper());

        //This is the endpoint to initiate a websocket connection
        registry.addEndpoint("/secured/user");
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        log.info("Successfully completed subscription for username: testUser");
    }

    @EventListener
    public void handleConnectEvent(SessionConnectEvent event) {
        log.info("Successfully established ws connection for username: testUser");
    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        log.info("Successfully removed ws connection for username: testUser");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            @SuppressWarnings("unchecked")
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                final StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
                final StompCommand command = headerAccessor.getCommand();
                MultiValueMap<String, String> nativeHeaders =
                        ((MultiValueMap<String, String>) message.getHeaders().get(NATIVE_HEADERS));
                if (CONNECT.equals(command) && Objects.nonNull(nativeHeaders)) {
                    //This is done to override the heart-beat so that the server can set whatever desired
                    nativeHeaders.put("heart-beat",
                            List.of("0," + 30000));
                }
                if (SUBSCRIBE.equals(command)) {
                    String username = "testUser";
                    if (Objects.nonNull(nativeHeaders)) {
                        //All these are rabbitmq queue arguments
                        nativeHeaders.put("auto-delete", List.of("false"));
                        nativeHeaders.put("durable", List.of("true"));
                        nativeHeaders.put("x-expires", List.of("2765000000"));
                        nativeHeaders
                                .put("x-message-ttl", List.of("2592000000"));
                        //stomp plugin in rabbitmq will create a queue with "custom_testUser"
                        nativeHeaders
                                .put("destination", List.of("/queue/custom_" + username.toLowerCase()));
                        nativeHeaders.put("id", List.of(username));
                    } else {
                        throw new IllegalStateException(
                                "Cannot find required subscription headers.");
                    }
                }
                return message;
            }
        });
    }
}
