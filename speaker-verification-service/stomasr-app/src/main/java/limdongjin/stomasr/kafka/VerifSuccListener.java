package limdongjin.stomasr.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import limdongjin.stomasr.service.SuccService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
@KafkaListener(
    topics = { KafkaConstants.TOPIC_INFER_VERIFICATION },
    concurrency = "1",
    groupId = KafkaConstants.GROUP_ID_II_GROUP
)
public class VerifSuccListener {
    private static final Logger logger = Logger.getLogger("succListener");

    private final SuccService succService;

    public VerifSuccListener(SuccService succService) {
        this.succService = succService;
    }

    @KafkaHandler
    public void onInfer(
        @Header(KafkaHeaders.OFFSET) Long offset,
        @Payload byte[] payload
    ) throws IllegalArgumentException, InvalidProtocolBufferException {
        if(payload == null){
            throw new IllegalArgumentException("receive null payload");
        }
        if(offset == null){
            throw new AssertionError("offset can not be null");
        }
        logger.info(String.format("onInfer %s && offset is %d", new String(payload), offset));

        try {
            succService.onInferVerif(payload);
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }
}
