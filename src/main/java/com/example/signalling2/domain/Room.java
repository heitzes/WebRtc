package com.example.signalling2.domain;

import com.example.signalling2.dto.Request.RoomCreateRequestDto;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Getter
@Setter
@RedisHash("ROOM")
public class Room {
    @Id
    @NonNull
    private String id;
    @NonNull
    private String title;
    @NonNull
    private String profileUrl;
    private String mediaPipeline; // refactor: 추가하기
    private String uuid;
    private LocalDateTime localDateTime;

    public Room(String id, String title, String profileUrl) {
        this.id = id;
        this.title = title;
        this.profileUrl = profileUrl;
        this.uuid = UUID.randomUUID().toString();
        this.localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0);
    }
}
