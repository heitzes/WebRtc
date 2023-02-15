package com.example.signalling2.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.io.Serializable;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class RoomRedis implements Serializable {
    private String title;
    private String profileUrl;
}
