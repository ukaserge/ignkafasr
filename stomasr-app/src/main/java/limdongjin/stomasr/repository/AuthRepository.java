//package limdongjin.stomasr.repository;

//import org.springframework.stereotype.Component;

//import java.util.concurrent.ConcurrentHashMap;

//@Component
//public class AuthRepository {
//    private final ConcurrentHashMap<String, String> m;
//
//    public AuthRepository() {
//        this.m = new ConcurrentHashMap<>();
//    }
//
//    public void putIfAbsent(String id, String value) {
//        m.putIfAbsent(id, value);
//    }
//
//    public boolean containsKey(String id) {
//        return m.containsKey(id);
//    }
//
//    public String getById(String id) {
//        return m.get(id);
//    }
//
//    public int size() {
//        return m.size();
//    }
//}
