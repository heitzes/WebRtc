package com.example.signalling2.domain;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;
import java.util.ArrayList;

@Getter
@Setter
public class Room {
    @Id
    private String id;
    private UserSession owner;
    private ArrayList<String> viewers;
    private Long viewCount;

    public Room(UserSession session) {
        this.owner = session;
        this.id = session.getId();
        this.viewers = new ArrayList<String>();
        this.viewCount = 0L;
    }
}
