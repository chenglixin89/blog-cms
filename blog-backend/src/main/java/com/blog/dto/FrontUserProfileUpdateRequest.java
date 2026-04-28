package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FrontUserProfileUpdateRequest {

    @NotBlank(message = "Nickname is required")
    @Size(max = 80, message = "Nickname must be less than 80 characters")
    private String nickname;

    @Size(max = 120, message = "Email must be less than 120 characters")
    private String email;

    @Size(max = 500, message = "Avatar must be less than 500 characters")
    private String avatar;

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
