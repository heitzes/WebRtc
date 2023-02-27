package com.example.signalling2.controller.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VodResponseDto {
    String liveSessionId;
    String roomId;
    String title;
    String profileUrl;
}
