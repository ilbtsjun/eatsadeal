package com.backend.auth.principal;

import com.backend.common.dto.UserRole;
import com.backend.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class AuthenticatedUser implements UserDetails {
    private final Long userId;
    private final UserRole role;

    public AuthenticatedUser(Long userId, UserRole role){
        this.userId = userId;
        this.role = role;
    }

    public static AuthenticatedUser from(User user){
        return new AuthenticatedUser(user.getId(), user.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getKey()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }
}
