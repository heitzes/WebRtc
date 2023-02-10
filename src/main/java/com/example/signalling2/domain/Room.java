package com.example.signalling2.domain;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;
import java.util.ArrayList;

@Getter
@Setter
public class Room {
    @Id
    private String id; // 유저 이메일로 변경
    private ArrayList<String> viewers;
    private Long viewCount;

    public Room(String email) {
        this.id = email;
        this.viewers = new ArrayList<String>();
        this.viewCount = 0L;
    }
}
