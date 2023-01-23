package limdongjin.stomasr.stomp;

import limdongjin.stomasr.dto.UserMessage;
import limdongjin.stomasr.service.JoinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class JoinSubReceiver {
    private final static Logger logger = LoggerFactory.getLogger(JoinSubReceiver.class);


    private JoinService joinService;

    public JoinSubReceiver(JoinService joinService){
        this.joinService = joinService;
    }

    /**
     *
     * sendSide: /app/join , {} , name
     * client subscribe: /user/queue/joinok
     */
    @MessageMapping("/join")
    public void join(
            @Header("simpSessionId") String sessionId,
            SimpMessageHeaderAccessor sha,
            @Payload UserMessage payload,
            Principal principal
    ) {
        logger.info("JOIN " + payload.getTargetUserName() + " && message = " + payload.getMessage() + " && sessionId = " + sessionId);
        if(principal == null){
            logger.info("Principal is null");
        }else {
            logger.info("Principal : " + principal.getName());
        }

        joinService.join(payload.getTargetUserName());
    }

    @GetMapping("/")
    public String index(){
        return "hello world !!!";
    }

    public void setJoinService(JoinService joinService) {
        this.joinService = joinService;
    }
}
