package com.example.signalling2.dto.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class RoomCreateRequestDto {
    @NonNull
    String roomId;
    @NonNull
    String title;
    @NonNull
    String profileUrl;
}
