package com.example.signalling2.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@RedisHash("ROOM")
@RequiredArgsConstructor
public class Room {
    @Id
    @NonNull
    private String id;
    @NonNull
    private String title;
    @NonNull
    private String profileUrl;
    private String mediaPipeline; // refactor: 추가하기
}
