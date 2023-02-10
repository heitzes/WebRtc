package com.example.signalling2.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED) //완전한 파라미터가 있어야 객체 생성 가능
public class ResponseDto<T> implements Serializable { // Serializable은 직렬화 사용
    private T data;

    public static <T> ResponseEntity<T> ok(T data) {
        return ResponseEntity.ok(data);
    }

    public static <T> ResponseEntity<T> created(T data) {
        return ResponseEntity
                .status(HttpStatus.CREATED) // study: CREATED, NO_CONTENT 같은건 HttpStatus 안에 정의되어 있음
                .body(data);
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build(); // study: build()는 "Build the response entity with no body"
    }

    public static ResponseEntity<Void> conflict() {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .build();
    }
}