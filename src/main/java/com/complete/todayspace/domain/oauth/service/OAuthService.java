package com.complete.todayspace.domain.oauth.service;

import com.complete.todayspace.domain.oauth.dto.OAuthDto;
import com.complete.todayspace.domain.user.entity.User;
import com.complete.todayspace.domain.user.entity.UserRole;
import com.complete.todayspace.domain.user.entity.UserState;
import com.complete.todayspace.domain.user.repository.UserRepository;
import com.complete.todayspace.global.jwt.JwtProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final JwtProvider jwtProvider;

    @Transactional
    public HttpHeaders kakao(String code, String KAKAO_CLIENT_ID) throws JsonProcessingException {

        String token = getToken(code, KAKAO_CLIENT_ID);
        OAuthDto oAuthDto = getKakaoUserInfo(token);
        User kakaoUser = registerOAuthUserIfNeeded(oAuthDto);

        String accessToken = jwtProvider.generateAccessToken(kakaoUser.getUsername(), kakaoUser.getRole().toString());
        String refreshToken = jwtProvider.generateRefreshToken(kakaoUser.getUsername(), kakaoUser.getRole().toString());
        ResponseCookie responseCookie = jwtProvider.createRefreshTokenCookie(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.add(HttpHeaders.SET_COOKIE, responseCookie.toString());

        kakaoUser.updateRefreshToken(refreshToken);
        userRepository.save(kakaoUser);

        return headers;
    }

    @Transactional
    public HttpHeaders naver(String code, String NAVER_CLIENT_ID, String NAVER_CLIENT_SECRET) throws JsonProcessingException {

        String token = getNaverToken(code, NAVER_CLIENT_ID, NAVER_CLIENT_SECRET);
        OAuthDto oAuthDto = getNaverUserInfo(token);
        User naverUser = registerOAuthUserIfNeeded(oAuthDto);

        String accessToken = jwtProvider.generateAccessToken(naverUser.getUsername(), naverUser.getRole().toString());
        String refreshToken = jwtProvider.generateRefreshToken(naverUser.getUsername(), naverUser.getRole().toString());
        ResponseCookie responseCookie = jwtProvider.createRefreshTokenCookie(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.add(HttpHeaders.SET_COOKIE, responseCookie.toString());

        naverUser.updateRefreshToken(refreshToken);
        userRepository.save(naverUser);

        return headers;
    }

    private String getToken(String code, String CLIENT_ID) throws JsonProcessingException {

        URI uri = UriComponentsBuilder.fromUriString("https://kauth.kakao.com")
                .path("/oauth/token")
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", CLIENT_ID);
        body.add("redirect_uri", "http://localhost:3000/oauth/kakao");
        body.add("code", code);

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity.post(uri)
                .headers(headers)
                .body(body);
        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);
        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());

        return jsonNode.get("access_token").asText();
    }

    private String getNaverToken(String code, String NAVER_CLIENT_ID, String NAVER_CLIENT_SECRET) throws JsonProcessingException {
        URI uri = UriComponentsBuilder.fromUriString("https://nid.naver.com/oauth2.0/token")
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", NAVER_CLIENT_ID)
                .queryParam("client_secret", NAVER_CLIENT_SECRET)
                .queryParam("redirect_uri", "http://localhost:3000/oauth/naver")
                .queryParam("code", code)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        RequestEntity<Void> requestEntity = RequestEntity.post(uri)
                .headers(headers)
                .build();
        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);
        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());

        return jsonNode.get("access_token").asText();
    }

    private OAuthDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {

        URI uri = UriComponentsBuilder.fromUriString("https://kapi.kakao.com")
                .path("/v2/user/me")
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity.post(uri)
                .headers(headers)
                .body(new LinkedMultiValueMap<>());
        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        Long id = jsonNode.get("id").asLong();
        String nickname = jsonNode.get("properties").get("nickname").asText();

        return new OAuthDto(id, nickname);
    }

    private OAuthDto getNaverUserInfo(String accessToken) throws JsonProcessingException {

        URI uri = UriComponentsBuilder.fromUriString("https://openapi.naver.com/v1/nid/me")
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        RequestEntity<Void> requestEntity = RequestEntity.get(uri)
                .headers(headers)
                .build();
        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        Long id = jsonNode.get("response").get("id").asLong();
        String nickname = jsonNode.get("response").get("name").asText();

        return new OAuthDto(id, nickname);
    }

    private User registerOAuthUserIfNeeded(OAuthDto oAuthDto) {

        User oAuthUser = userRepository.findByoAuthId(oAuthDto.getId()).orElse(null);

        if (oAuthUser == null) {

            String password = UUID.randomUUID().toString();
            String encryptedPassword = passwordEncoder.encode(password);
            oAuthUser = new User(oAuthDto.getNickname() + oAuthDto.getId(), encryptedPassword, null, UserRole.USER, UserState.ACTIVE, oAuthDto.getId());

        }

        return oAuthUser;
    }

}
