package limdongjin.stomasr.service;

import com.google.protobuf.InvalidProtocolBufferException;
import limdongjin.stomasr.protos.InferProto;
//import limdongjin.stomasr.repository.AuthRepository;
import limdongjin.stomasr.stomp.StompConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SuccService {
    private static final Logger logger = LoggerFactory.getLogger(SuccService.class.getName());
    private final SimpMessageSendingOperations messageSendingOperations;
    public SuccService(SimpMessageSendingOperations messageSendingOperations){
        this.messageSendingOperations = messageSendingOperations;
    }

    public void onInferVerif(byte[] payload) throws IllegalArgumentException, InvalidProtocolBufferException {
        logger.info("onInferVerif");

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
        messageSendingOperations.convertAndSendToUser(userId, StompConstants.VERIF_SUCC, msg);
    }

    // TODO refactoring duplicated logic
    public void onInferTranscribe(byte[] payload) throws IllegalArgumentException, InvalidProtocolBufferException {
        logger.info("onInferTranscribe");

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

        logger.info("convertAndSend /user/{}/topic/succ/transcribe", userId);
        messageSendingOperations.convertAndSendToUser(userId, StompConstants.TRANSCRIBE_SUCC, msg);
    }
}
