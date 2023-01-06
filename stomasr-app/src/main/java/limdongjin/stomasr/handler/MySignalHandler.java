package limdongjin.stomasr.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import limdongjin.stomasr.dto.WebSocketMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Qualifier("MySignalHandler")
@Component
public class MySignalHandler extends TextWebSocketHandler {
    private Set<WebSocketSession> sessionSet = new ConcurrentHashMap<Object, Object>().newKeySet();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MSG_TYPE_OFFER = "offer";
    private static final String MSG_TYPE_ANSWER = "answer";
    private static final String MSG_TYPE_ICE = "ice";
    private static final String MSG_TYPE_JOIN = "join";
    private static final String MSG_TYPE_LEAVE = "leave";

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("[ws] Session has been closed with status [{} {}]", status, session);
        sessionSet.remove(session);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionSet.add(session);
        try {
            session.sendMessage(new TextMessage(session.getId()));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        System.out.println("HANDLE TEXT MSG");
        System.out.println(session);
        System.out.println(textMessage);
        try {
            WebSocketMessageDto msg = null;
            try {
                msg = objectMapper.readValue(textMessage.getPayload(), WebSocketMessageDto.class);
                session.sendMessage(new TextMessage(textMessage.getPayload()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            logger.debug("[ws] msg = " + msg);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}