package limdongjin.stomasr.stomp;

import limdongjin.stomasr.dto.UserMessage;
import limdongjin.stomasr.repository.AuthRepository;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JoinSubReceiver {
    private final SimpMessageSendingOperations template;
    private final AuthRepository repository;
    public JoinSubReceiver(SimpMessageSendingOperations template, AuthRepository repository) {
        this.template = template;
        this.repository = repository;
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
            @Payload UserMessage msg
    ) {
        System.out.println("JOIN");
        System.out.println(msg);
        System.out.println(sessionId);

        if(repository.containsKey(msg.getTargetUserName())){
            template.convertAndSend("/topic/succ/"+ msg.getTargetUserName(), repository.getById(msg.getTargetUserName()));
        }

        template.convertAndSend("/topic/joinok/"+msg.getTargetUserName(), "HELLO; ");
    }

    @GetMapping("/")
    public String index(){
        return "hello world !!!";
    }
}
