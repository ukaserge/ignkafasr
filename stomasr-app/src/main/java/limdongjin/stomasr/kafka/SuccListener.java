package limdongjin.stomasr.kafka;

import limdongjin.stomasr.service.SuccService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import limdongjin.stomasr.kafka.KafkaConstants;
import java.util.logging.Logger;

@Component
@KafkaListener(
    topics = { KafkaConstants.TOPIC_INFER },
    concurrency = "3",
    groupId = KafkaConstants.GROUP_ID_MY_GROUP
)
public class SuccListener {
    private static final Logger logger = Logger.getLogger("succListener");

    private SuccService succService;

    public SuccListener() {}
    public SuccListener(SuccService succService) {
        this.succService = succService;
    }

    @KafkaHandler
    public void onInfer(
        @Header(KafkaHeaders.OFFSET) Long offset,
        @Payload String payload
    ) throws IllegalArgumentException
    {
        if(payload == null){
            throw new IllegalArgumentException("receive null payload");
        }
        if(offset == null){
            throw new AssertionError("offset can not be null");
        }
        logger.info(String.format("onInfer %s && offset is %d", payload, offset));

        succService.onInfer(payload);
    }

    @KafkaHandler(isDefault = true)
    public void onInfer(@Payload Object payload) {
        if(payload == null){
            throw new IllegalArgumentException("receive null payload");
        }
        logger.info("(default) receive payload: ");
        logger.info(payload.toString());

        try {
            if(Class.forName(String.class.getName()).isInstance(payload)){
                succService.onInfer((String) payload);
            }
        } catch (ClassNotFoundException exception){
            throw new AssertionError("false");
        }
    }
//    @KafkaListener(topics = KafkaTopicConstants.INFER_POSITIVE, concurrency = "3", groupId = CONSUMER_GROUP_ID)
//    public void onInferPositive(@NonNull @Payload String reqId) throws IllegalArgumentException {
//        logger.info("INFER POSITIVE ");
//        logger.info(reqId);
//
//        succService.onInferPositive(reqId);
//    }
//
//    @KafkaListener(topics = KafkaTopicConstants.INFER_POSITIVE, concurrency = "3", groupId = CONSUMER_GROUP_ID)
//    public void onInferPositive(@Nullable @Payload Object reqId) {
//        logger.info("doNothing");
//    }
//
//    @KafkaListener(topics = KafkaTopicConstants.INFER_NEGATIVE, concurrency = "3", groupId = CONSUMER_GROUP_ID)
//    public void onInferNegative(@Payload String reqId) throws IllegalArgumentException {
//        logger.info("INFER NEGATIVE ");
//        logger.info(reqId);
//
//        succService.onInferNegative(reqId);
//    }

    public SuccService getSuccService() {
        return succService;
    }

    public void setSuccService(SuccService succService) {
        this.succService = succService;
    }
}
