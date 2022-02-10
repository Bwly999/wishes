package cn.edu.xmu.wishes.user.controller;

import cn.edu.xmu.wishes.user.model.Chat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatController {
    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @Autowired
    private SimpMessagingTemplate brokerMessagingTemplate;

    @MessageMapping("/chat/private")
    public Chat privateChat(SimpMessageHeaderAccessor sha, @Payload Chat content, Principal principal) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(content));

        this.brokerMessagingTemplate.convertAndSendToUser(content.getReceiver(), "/topic/chat", content);
        return content;
    }

    @MessageMapping("/chat/public") //这里是客户端发送消息对应的路径，等于configureMessageBroker中配置的setApplicationDestinationPrefixes + 这路径即 /app/sendPublicMessage
    @SendTo("/topic/chat") //也可以使用 messagingTemplate.convertAndSend(); 推送
    public Chat sendPublicMessage(@Payload Chat chatMessage) {
        return chatMessage;
    }
}
