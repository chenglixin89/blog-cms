package com.blog.dto;

public record FrontUserProfileResponse(
    Long id,
    String username,
    String nickname,
    String email,
    String avatar,
    String bio
) {
}
