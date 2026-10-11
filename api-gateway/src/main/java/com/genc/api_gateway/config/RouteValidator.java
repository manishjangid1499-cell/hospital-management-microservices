package com.genc.api_gateway.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Endpoints that don't require authentication
    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/validate",
            "/api/auth/forgot-password",
            "/api/patients/register",
            "/actuator/health",
            "/actuator/info",
            "/eureka/**"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().getPath();
                return openApiEndpoints.stream()
                        .noneMatch(pattern -> pathMatcher.match(pattern, path) || path.equals(pattern));
            };
}

