package com.example.signalling2.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@RedisHash("USER")
@RequiredArgsConstructor
public class User {
    @Id
    @NonNull
    private String id;
    @NonNull
    private String roomId;
    private String sessionId;
    @NonNull
    private String webRtcEndpoint;
}
