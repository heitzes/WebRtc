package com.example.signalling2.domain;
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
