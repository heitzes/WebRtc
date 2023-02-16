package com.example.signalling2.dto.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreateRequestDto {
    String roomId;
    String title;
    String profileUrl;
}
