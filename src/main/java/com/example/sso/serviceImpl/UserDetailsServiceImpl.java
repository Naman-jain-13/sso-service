package com.example.sso.serviceImpl;

import com.example.sso.model.User;
import com.example.sso.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserDetailsServiceImpl implements UserDetailsService, UserService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> users = new HashMap<>();

    public UserDetailsServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        users.put("john", passwordEncoder.encode("password123"));
        users.put("jane", passwordEncoder.encode("password456"));
        users.put("naman", passwordEncoder.encode("naman@sso123"));
    }

    @Override
    public User authenticate(String username, String password) {
        String storedPassword = users.get(username);
        if (storedPassword != null && passwordEncoder.matches(password, storedPassword)) {
            return new User(username, null);
        }
        return null;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String storedPassword = users.get(username);
        if (storedPassword == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password(storedPassword)
                .roles("USER")
                .build();
    }
}
