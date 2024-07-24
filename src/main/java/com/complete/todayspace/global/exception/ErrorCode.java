package com.complete.todayspace.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    // Common
    FAIL(500, "실패했습니다."),
    INVALID_REQUEST(400, "입력값을 확인해주세요."),
    INVALID_URL_ACCESS(400, "잘못된 URL 접근입니다."),
    UNAUTHENTICATED(401, "로그인 후 이용해주세요."),
    UNAUTHORIZED_ADMIN(403, "권한이 없는 사용자입니다."),
    // User
    USER_NOT_UNIQUE(409, "사용 중인 아이디입니다."),
    CHECK_USERNAME_PASSWORD(400, "아이디, 비밀번호를 확인해주세요."),
    USER_NOT_FOUND(400, "해당 유저를 찾을 수 없습니다."),
    // Products
    PRODUCT_NOT_FOUND(404,"해당 상품을 찾을 수 없습니다."),
    NOT_OWNER_PRODUCT(403, "작성자만 변경할 수 있습니다."),

    // Posts
    POST_NOT_FOUND(404,"해당 상품을 찾을 수 없습니다."),
    NOT_OWNER_POST(403, "작성자만 변경할 수 있습니다."),

    // Hastags

    // Chats

    // JWT
    TOKEN_EXPIRED(401, "토큰이 만료되었습니다."),
    INVALID_TOKEN(401, "잘못된 JWT 토큰입니다."),
    TOKEN_MISMATCH(401, "토큰이 일치하지 않습니다."),
    TOKEN_NOT_FOUND_FOR_COOKIE(401, "쿠키에 토큰이 존재하지 않습니다.");;

    private final Integer statusCode;
    private final String message;

}
