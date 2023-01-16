
package com.example.signalling2.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import java.util.concurrent.ConcurrentHashMap;

@RedisHash("room-table")
@Getter
public class RoomSession {
    @Id
    private String id;

    private ConcurrentHashMap<String, UserSession> viewers;

    @Builder
    public RoomSession(String id, ConcurrentHashMap viewers) {
        this.id = id;
        this.viewers = viewers;
    }
}
