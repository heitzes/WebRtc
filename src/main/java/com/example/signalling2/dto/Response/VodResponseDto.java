package com.example.signalling2.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VodResponseDto {
    String uuid;
    String roomId;
    String title;
    String profileUrl;
}
