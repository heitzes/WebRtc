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
public class SessionRedis implements Serializable {
    private String roomId;
    private String email;

}
