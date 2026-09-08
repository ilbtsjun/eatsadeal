package com.backend.auth.filter;


import com.backend.auth.principal.AuthenticatedUser;
import com.backend.auth.resolver.BearerTokenResolver;
import com.backend.auth.token.JwtTokenProvider;
import com.backend.auth.token.TokenBlacklist;
import com.backend.common.error.BusinessException;
import com.backend.common.error.ErrorCode;
import com.backend.user.entity.User;
import com.backend.user.repository.UserRepository;
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

        if (token != null
                && jwtTokenProvider.validateToken(token)
                && !tokenBlacklist.contains(token)) {

            Long userId = jwtTokenProvider.getUserId(token);

            User user = userRepository.findById(userId)
                    .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND));

            AuthenticatedUser principal = AuthenticatedUser.from(user);

            Authentication authentication = new UsernamePasswordAuthenticationToken(principal,
                    null,
                    principal.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
