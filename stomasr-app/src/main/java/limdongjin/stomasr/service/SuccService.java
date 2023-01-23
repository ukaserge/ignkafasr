package limdongjin.stomasr.service;

import com.google.protobuf.InvalidProtocolBufferException;
import limdongjin.stomasr.protos.InferProto;
//import limdongjin.stomasr.repository.AuthRepository;
import limdongjin.stomasr.stomp.MessageDestinationPrefixConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.logging.Level;

@Component
public class SuccService {
    private static final Logger logger = LoggerFactory.getLogger(SuccService.class.getName());
    private final SimpMessageSendingOperations messageSendingOperations;
//    private final AuthRepository authRepository;

    public SuccService(SimpMessageSendingOperations messageSendingOperations){
        this.messageSendingOperations = messageSendingOperations;
    }

    public void onInfer(byte[] payload) throws IllegalArgumentException, InvalidProtocolBufferException {
        logger.info("onInfer");

        InferProto.Infer infer = InferProto.Infer.parseFrom(payload);

        logger.info(infer.toString());

        if(!infer.isInitialized()){
            throw new InvalidProtocolBufferException("invalid protocol buffer");
        }

        String userId = infer.getUserId();
        String reqId = infer.getReqId();
        String msg = infer.toString();

        // check uuid format
        try {
            UUID uuid = UUID.fromString(reqId);
            UUID uuid2 = UUID.fromString(userId);
        } catch (IllegalArgumentException exception){
            logger.error("invalid uuid format");
            throw exception;
        }

        if(infer.getInferResult().isBlank()){
            throw new IllegalArgumentException("invalid message format");
        }

        logger.info("convertAndSend /user/{}/topic/succ", userId);
//        messageSendingOperations.convertAndSend(MessageDestinationPrefixConstants.SUCC + reqId, msg);
//        authRepository.putIfAbsent(reqId, msg);

        messageSendingOperations.convertAndSendToUser(userId, "/topic/succ", msg);
    }
}
