package com.example.signalling2.domain;

import com.example.signalling2.service.UserService;
import lombok.Builder;
import org.apache.catalina.User;
import org.springframework.web.socket.WebSocketSession;

import javax.persistence.Id;
import java.util.ArrayList;

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


    public String getId() {
        return id;
    }

    public UserSession getOwner() {
        return owner;
    }
    public ArrayList<String> getViewers() {
        return viewers;
    }

    public void setViewers(ArrayList<String> viewers) {
        this.viewers = viewers;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }
}
