package limdongjin.stomasr.stomp;

import limdongjin.stomasr.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class MessageReceiver {
    private final static Logger logger = LoggerFactory.getLogger(MessageReceiver.class);

    private final SimpMessageSendingOperations messageSendingOperations;

    public MessageReceiver(SimpMessageSendingOperations messageSendingOperations) {
        this.messageSendingOperations = messageSendingOperations;
    }

    @MessageMapping("/enter")
    public void enterRoom(
            @Header("simpSessionId") String sessionId,
            SimpMessageHeaderAccessor sha,
            @Payload ChatMessage chatMessage,
            Principal principal
    ){
       logger.info("room enter!");
       chatMessage.setMessage("Enter " + chatMessage.getSender());
       messageSendingOperations.convertAndSend(
               "/topic/room/" + chatMessage.getRoomId(),
               chatMessage
       );
    }

    @MessageMapping("/message")
    public void message(
            @Header("simpSessionId") String sessionId,
            SimpMessageHeaderAccessor sha,
            @Payload ChatMessage chatMessage,
            Principal principal
    ){
        logger.info("message!");
        messageSendingOperations.convertAndSend(
                "/topic/room/"+chatMessage.getRoomId(),
                chatMessage
        );
    }
}
