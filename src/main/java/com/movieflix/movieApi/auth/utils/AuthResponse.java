package com.movieflix.movieApi.auth.utils;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;

    private String refreshToken;
    private String name;
    private String email;
}
