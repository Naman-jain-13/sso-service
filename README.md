# SSO Auth Service

A Single Sign-On (SSO) authentication service built with **Spring Boot 3**, **Spring Security**, and **JWT (JJWT)**.

## Tech Stack

- **Java 17** + **Spring Boot 3.2**
- **Spring Security** — SecurityFilterChain, BCrypt password hashing, RBAC
- **JJWT 0.12** — HS512-signed JSON Web Tokens
- **Thymeleaf** — server-side rendered login/home pages
- **Maven** — build and dependency management

## Features

- Stateless JWT-based authentication with HttpOnly cookie transport
- Custom `UserDetailsService` and `JwtTokenService` for credential validation and token lifecycle
- Role-based access control (RBAC)
- Login, home, and logout flows with Thymeleaf templates
- BCrypt password hashing

## Project Structure

```
src/main/java/com/example/sso/
├── config/          SecurityConfig (filter chain, BCrypt bean)
├── controller/      SSOController (login, home, logout endpoints)
├── model/           User POJO
├── service/         JwtTokenService, UserService interfaces
├── serviceImpl/     JwtTokenServiceImpl, UserDetailsServiceImpl
└── SsoServiceApplication.java
```

## Getting Started

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run
```

Open http://localhost:8080/login — test credentials: `john / password123` or `jane / password456`.

## Configuration

Edit `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `spring.security.jwt.secret-key` | (64-byte key) | HMAC-SHA512 signing key |
| `spring.security.jwt.expiration` | `3600000` | Token TTL in milliseconds (1 hour) |
