package com.example.signalling2.dto.Request;

import lombok.Getter;

@Getter
public class RoomCreateRequestDto {
    String roomId;
    String title;
    String profileUrl;
}
