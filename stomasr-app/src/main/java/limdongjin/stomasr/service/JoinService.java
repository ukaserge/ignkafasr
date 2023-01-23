package limdongjin.stomasr.service;

import limdongjin.stomasr.stomp.MessageDestinationPrefixConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
public class JoinService {
    final static Logger logger = LoggerFactory.getLogger(JoinService.class);
    private final SimpMessageSendingOperations messageSendingOperations;
//    private final AuthRepository authRepository;

    public JoinService(final SimpMessageSendingOperations messageSendingOperations){
        this.messageSendingOperations = messageSendingOperations;
    }

    public void join(final String userName){
        logger.info("JOIN "+userName);
//        if(authRepository.containsKey(userName)){
//            messageSendingOperations.convertAndSend(MessageDestinationPrefixConstants.SUCC + userName, authRepository.getById(userName));
//        }

        messageSendingOperations.convertAndSend(MessageDestinationPrefixConstants.JOINOK + userName, "HELLO; ");
    }
}
