
package com.example.signalling2.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import java.util.concurrent.ConcurrentHashMap;

public class RoomSession {
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getView() {
        return view;
    }

    public void setView(Long view) {
        this.view = view;
    }

    public void addView() {
        ++this.view;
    }

    public void subView() {
        --this.view;
    }

    @Id
    private String id;
    private Long view;
    @Builder
    public RoomSession(String id) {
        this.id = id;
        this.view = 0L;
    }
}
