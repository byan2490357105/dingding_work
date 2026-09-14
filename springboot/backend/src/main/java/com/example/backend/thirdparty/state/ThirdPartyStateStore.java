package com.example.backend.thirdparty.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 授权 state 存储（内存版，10 分钟有效，一次性使用）。
 * 单机部署足够；多实例部署时可替换为 Redis 实现。
 */
@Component
public class ThirdPartyStateStore {

    private static final long TTL_MILLIS = 10 * 60 * 1000L;

    private final Map<String, ThirdPartyState> store = new ConcurrentHashMap<>();

    public String create(String provider, String mode, Long userId) {
        clearExpired();
        String state = UUID.randomUUID().toString().replace("-", "");
        ThirdPartyState value = new ThirdPartyState();
        value.setProvider(provider);
        value.setMode(mode);
        value.setUserId(userId);
        value.setExpireAt(System.currentTimeMillis() + TTL_MILLIS);
        store.put(state, value);
        return state;
    }

    public ThirdPartyState consume(String state) {
        if (state == null) {
            return null;
        }
        ThirdPartyState value = store.remove(state);
        if (value == null || value.getExpireAt() < System.currentTimeMillis()) {
            return null;
        }
        return value;
    }

    private void clearExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().getExpireAt() < now);
    }
}
