package com.example.signalling2;
import static org.junit.jupiter.api.Assertions.*;

import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.RoomRedis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


@DisplayName("RedisTemplate basic 테스트")
@SpringBootTest
@ActiveProfiles("local")
public class RedisTest {


    @DisplayName("RedisTemplate hash CRUD 테스트")
    @Nested

    class RedisTemplateCrudTest {

        @Autowired
        private RedisTemplate<String, RoomRedis> redisRoomTemplate;
        @Autowired
        private RedisTemplate<String, String> redisViewTemplate;

        @Test
        void getSetTest() {
            RoomRedis roomRedis1 = new RoomRedis("welcome~", "https");
            RoomRedis roomRedis2 = new RoomRedis("dd~", "d");
            redisRoomTemplate.opsForHash().put("room", "artist1", roomRedis1);
            redisRoomTemplate.opsForHash().put("room", "artist2", roomRedis2);
            for (Object roomId : redisRoomTemplate.opsForHash().keys("room")) {
                System.out.println(redisRoomTemplate.opsForHash().get("room", roomId));
            }

//            redisViewTemplate.opsForSet().add("artist", "23424");
//            redisViewTemplate.opsForSet().add("artist", "22");
//            redisViewTemplate.opsForSet().add("artist2", "1");
//            System.out.println(redisViewTemplate.opsForSet().isMember("artist", "22"));
//
//
//
//            RoomRedis testHash = (RoomRedis) redisRoomTemplate.opsForHash().get("room", "artist");
//            System.out.println(redisRoomTemplate.opsForSet().members("artist").toString());
//            redisRoomTemplate.opsForSet().remove("artist", "463");
//            System.out.println(redisRoomTemplate.opsForSet().members("artist").toString());

        }
    }
}