package com.backend.auth.filter;


import com.backend.auth.principal.AuthenticatedUser;
import com.backend.auth.resolver.BearerTokenResolver;
import com.backend.auth.token.JwtTokenProvider;
import com.backend.auth.token.TokenBlacklist;
import com.backend.user.dto.UserStatus;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
    throws ServletException, IOException {

        String token = BearerTokenResolver.resolve(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtTokenProvider.validateToken(token) || tokenBlacklist.contains(token)) {
                clearAuthentication();
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = jwtTokenProvider.getUserId(token);

            Optional<User> optionalUser = userRepository.findById(userId);

            if (optionalUser.isEmpty() || optionalUser.get().getUserStatus() != UserStatus.ACTIVE) {
                clearAuthentication();
                filterChain.doFilter(request, response);
                return;
            }

            AuthenticatedUser principal = AuthenticatedUser.from(optionalUser.get());

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            token,
                            principal.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException e) {
            clearAuthentication();
        }

        filterChain.doFilter(request, response);
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
