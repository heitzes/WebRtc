package com.example.signalling2;

import com.example.signalling2.domain.People;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.repository.PeopleRedisRepository;
import com.example.signalling2.repository.RoomRedisRepository;
import com.example.signalling2.repository.UserRedisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisDataApplicationTests {

    @Autowired
    private UserRedisRepository userRedisRepository;

//    @Autowired
//    private RoomRedisRepository roomRedisRepository;

    @Test
    public void saveTest() {
        //given

        //when
        //People savePeople = peopleRedisRepository.save(people);
        //UserSession saveUser = userRedisRepository.save(presenter);
        //then
        //Optional<People> findPeople = peopleRedisRepository.findById(savePeople.getId());
        //Optional<UserSession> findUser = userRedisRepository.findById(saveUser.getId());
        //assertThat(findUser.isPresent()).isEqualTo(Boolean.TRUE);
    }
}