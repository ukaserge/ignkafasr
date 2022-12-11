package limdongjin.stomasr.handler;

//import org.springframework.context.ApplicationListener;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.web.socket.messaging.SessionSubscribeEvent;
//
//import java.security.Principal;
//import java.util.List;
//import java.util.Map;
//
//public class WebSocketSubscribeHandler<S> implements ApplicationListener<SessionSubscribeEvent> {
//
//    private SimpMessagingTemplate messageTemplate;
//
//    public WebSocketSubscribeHandler() {
//        super();
//    }
//
//    public WebSocketSubscribeHandler(SimpMessagingTemplate messageTemplate) {
//        this.messageTemplate = messageTemplate;
//    }
//
//    @Override
//    public void onApplicationEvent(SessionSubscribeEvent event) {
//        String source = String.valueOf(((List) ((Map) event.getMessage().getHeaders().get("nativeHeaders")).get("destination")).get(0));
//
//        if (source.contains(TARGET)) {
//            Principal user = event.getUser();
//            if (user == null || user.getName() == null) {
//                System.out.println("USER IS NULL");
//                return;
//            }
//
//            messageTemplate.convertAndSendToUser(
//                    user.getName(),
//                    "/topic/succ",
//                    event.getMessage()
//            );
//        }
//    }
//}