package com.example.signalling2;

import com.example.signalling2.domain.*;
import com.example.signalling2.repository.ViewRepository;
import com.example.signalling2.repository.RoomRepository;
import com.example.signalling2.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;


@DisplayName("RedisTemplate basic 테스트")
@SpringBootTest
@ActiveProfiles("local")
public class RedisTest {


    @DisplayName("RedisTemplate hash CRUD 테스트")
    @Nested

    class RedisTemplateCrudTest {

//        @Autowired
//        private RedisTemplate<String, String> redisViewTemplate;
//
//        @Autowired
//        private ViewRepository viewRepository;
//        @Autowired
//        private UserRepository userRepository;
//        @Autowired
//        private RoomRepository roomRepository;

        @Test
        void getSetTest() {
//            System.out.println(roomRepository.findAll());
//            Optional<Room> findRoom1 = roomRepository.findById("artist1");
//            roomRepository.delete(findRoom1.get());
//            System.out.println(roomRepository.findAll());
//            // given
//            Room room1 = new Room("artist1", "hi", "http:ss", "media", "session");
//            Room room2 = new Room("artist2", "hi", "http:ss", "media", "session");
//            User user1 = new User("artist1", "artist1", "session1", "endpoint");
//            User user2 = new User("fan1", "artist1", "session1", "endpoint");
//            User user3 = new User("fan2", "artist1", "session1", "endpoint");
//
//            // when
//            Room saveRoom1 = roomRepository.save(room1);
//            Room saveRoom2 = roomRepository.save(room2);
//            User saveUser1 = userRepository.save(user1);
//            User saveUser2 = userRepository.save(user2);
//            User saveUser3 = userRepository.save(user3);
//
//            // then
//            Optional<Room> findRoom1 = roomRepository.findById(saveRoom1.getId());
//            System.out.println(findRoom1.get().getProfileUrl());
//            viewRepository.save(saveUser2.getRoomId(), saveUser2.getId());
//            viewRepository.save(saveUser3.getRoomId(), saveUser3.getId());








//            RoomInfo roomInfo1 = new RoomInfo("welcome~", "https");
//            RoomInfo roomInfo2 = new RoomInfo("dd~", "d");
//            redisRoomTemplate.opsForHash().put("room", "artist1", roomInfo1);
//            redisRoomTemplate.opsForHash().put("room", "artist2", roomInfo2);
//            for (Object roomId : redisRoomTemplate.opsForHash().keys("room")) {
//                System.out.println(redisRoomTemplate.opsForHash().get("room", roomId));
//            }
//
//            redisViewTemplate.opsForSet().add("user:artist", "23424");
//            redisViewTemplate.opsForSet().add("user:artist", "22");
//            redisViewTemplate.opsForSet().add("user:artist2", "1");
//            System.out.println(redisViewTemplate.opsForSet().isMember("artist", "22"));
//
//
//
//            RoomRedis testHash = (RoomRedis) redisRoomTemplate.opsForHash().get("room", "artist");
//            System.out.println(redisRoomTemplate.opsForSet().members("artist").toString());
//            redisRoomTemplate.opsForSet().remove("artist", "463");
//            System.out.println(redisRoomTemplate.opsForSet().members("artist").toString());

        }
//        @Test
//        public void basicSave() {
//            // given
//            Person hk = new Person("hk", "first", "last");
//            Person is = new Person("is", "first", "last");
//
//            // when
//            Person savedHk = redisRepository.save(hk);
//            Person savedIs = redisRepository.save(is);
//
//
//            // then
//            Optional<Person> findHk = redisRepository.findById(savedHk.getId());
//
//            System.out.println(findHk.get().getFirstname());
//            findHk.get().setFirstname("changed");
//
//        }
    }
}