package com.example.backend.thirdparty.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 第三方登录回跳前端的一次性票据（内存版，2 分钟有效）。
 * 避免把 JWT 直接暴露在浏览器地址栏中。
 */
@Component
public class ThirdPartyTicketStore {

    private static final long TTL_MILLIS = 2 * 60 * 1000L;

    private final Map<String, TicketValue> store = new ConcurrentHashMap<>();

    public String create(Long userId) {
        clearExpired();
        String ticket = UUID.randomUUID().toString().replace("-", "");
        store.put(ticket, new TicketValue(userId, System.currentTimeMillis() + TTL_MILLIS));
        return ticket;
    }

    public Long consume(String ticket) {
        if (ticket == null) {
            return null;
        }
        TicketValue value = store.remove(ticket);
        if (value == null || value.expireAt < System.currentTimeMillis()) {
            return null;
        }
        return value.userId;
    }

    private void clearExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expireAt < now);
    }

    private static class TicketValue {
        private final Long userId;
        private final long expireAt;

        private TicketValue(Long userId, long expireAt) {
            this.userId = userId;
            this.expireAt = expireAt;
        }
    }
}
