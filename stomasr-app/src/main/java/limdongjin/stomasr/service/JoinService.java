package limdongjin.stomasr.service;

import limdongjin.stomasr.repository.AuthRepository;
import limdongjin.stomasr.stomp.MessageDestinationPrefixConstants;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
public class JoinService {
    private final SimpMessageSendingOperations messageSendingOperations;
    private final AuthRepository authRepository;

    public JoinService(final SimpMessageSendingOperations messageSendingOperations, final AuthRepository authRepository){
        this.authRepository = authRepository;
        this.messageSendingOperations = messageSendingOperations;
    }

    public void join(final String userName){
        if(authRepository.containsKey(userName)){
            messageSendingOperations.convertAndSend(MessageDestinationPrefixConstants.SUCC + userName);
        }

        messageSendingOperations.convertAndSend(MessageDestinationPrefixConstants.JOINOK + userName, "HELLO; ");
    }
}
