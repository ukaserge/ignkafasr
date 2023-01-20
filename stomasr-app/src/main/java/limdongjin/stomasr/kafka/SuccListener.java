package limdongjin.stomasr.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
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
import limdongjin.stomasr.protos.InferProto;

@Component
@KafkaListener(
    topics = { KafkaConstants.TOPIC_INFER },
    concurrency = "3",
    groupId = KafkaConstants.GROUP_ID_MY_GROUP
)
public class SuccListener {
    private static final Logger logger = Logger.getLogger("succListener");

    private SuccService succService;

    public SuccListener(SuccService succService) {
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
            succService.onInfer(payload);
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }
}
