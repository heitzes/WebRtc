package com.example.signalling2.common;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED) //완전한 파라미터가 있어야 객체 생성 가능
public class Response<T> implements Serializable { // Serializable은 직렬화 사용

    public static <T> ResponseEntity<T> ok(T data) {
        return ResponseEntity.ok(data);
    }

    public static <T> ResponseEntity<T> created(T data) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(data);
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}