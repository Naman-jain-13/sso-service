package com.example.sso.service;

import com.example.sso.model.User;

public interface JwtTokenService {

    String generateToken(User user);

    boolean validateToken(String token);

    String getUsernameFromToken(String token);
}
