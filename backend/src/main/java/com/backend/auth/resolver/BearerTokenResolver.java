package com.backend.auth.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;


public final class BearerTokenResolver {
    private static final String BEARER_PREFIX = "Bearer ";

    private BearerTokenResolver(){}

    public static String resolve(HttpServletRequest request){
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if(!StringUtils.hasText(authorization)){
            return null;
        }

        if(!authorization.startsWith(BEARER_PREFIX)){
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();

        return token.isBlank() ? null : token;
    }
}
