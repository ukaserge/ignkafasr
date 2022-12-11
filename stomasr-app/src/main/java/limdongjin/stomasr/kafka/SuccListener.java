package limdongjin.stomasr.kafka;

import limdongjin.stomasr.repository.AuthRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
public class SuccListener {
    SimpMessageSendingOperations template;
    AuthRepository repository;
    public SuccListener(SimpMessageSendingOperations template, AuthRepository repository) {
        this.template = template;
        this.repository = repository;
    }

    @KafkaListener(
            topics = "infer-positive", concurrency = "3", groupId = "my-group"
    )
    public void inferPositive(@Payload String reqId){
        System.out.println("INFER POSITIVE");
        System.out.println(reqId);

        // Inference 성공 결과 전송
        template.convertAndSend("/topic/succ/"+reqId, "OK; ");
        repository.m.putIfAbsent(reqId, "OK; ");
    }

    @KafkaListener(topics = "infer-negative", concurrency = "3", groupId = "my-group")
    public void inferNegative(@Payload String reqId){
        System.out.println("INFER NEGATIVE");
        System.out.println(reqId);

        // Inference 실패 결과 전송
        template.convertAndSend("/topic/succ/"+reqId, "FAIL; ");
        repository.m.putIfAbsent(reqId, "FAIL; ");
    }
}
