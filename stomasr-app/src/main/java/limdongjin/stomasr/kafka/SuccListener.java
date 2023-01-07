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

    @KafkaListener(topics = "infer-positive", concurrency = "3", groupId = "my-group")
    public void onInferPositive(@Payload String reqId){
        System.out.println("INFER POSITIVE");
        System.out.println(reqId);

        // Inference 성공 결과 전송
        String msg = "OK; ";
        template.convertAndSend("/topic/succ/"+reqId, msg);
        repository.putIfAbsent(reqId, msg);
    }

    @KafkaListener(topics = "infer-negative", concurrency = "3", groupId = "my-group")
    public void onInferNegative(@Payload String reqId){
        System.out.println("INFER NEGATIVE");
        System.out.println(reqId);

        // Inference 실패 결과 전송
        String msg = "FAIL; ";
        template.convertAndSend("/topic/succ/"+reqId, msg);
        repository.putIfAbsent(reqId, msg);
    }
}
