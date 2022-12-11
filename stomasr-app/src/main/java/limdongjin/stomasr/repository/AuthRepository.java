package limdongjin.stomasr.repository;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRepository {
    public ConcurrentHashMap<String, String> m;

    public AuthRepository() {
        this.m = new ConcurrentHashMap<>();
    }
}
