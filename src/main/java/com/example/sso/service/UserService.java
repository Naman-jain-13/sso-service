package com.example.sso.service;

import com.example.sso.model.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserService {

    User authenticate(String username, String password);

    UserDetails loadUserByUsername(String username);
}
