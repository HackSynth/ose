package com.ose.auth;

import com.ose.common.config.JwtService;
import com.ose.common.exception.BusinessException;
import com.ose.common.exception.NotFoundException;
import com.ose.model.AppUser;
import com.ose.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtService.generateToken(user.getUsername(), user.getDisplayName(), user.getRole().name());
        return new AuthDtos.AuthResponse(token, toProfile(user));
    }

    public AuthDtos.UserProfile currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new NotFoundException("未找到当前用户");
        }
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("未找到当前用户"));
        return toProfile(user);
    }

    private AuthDtos.UserProfile toProfile(AppUser user) {
        return new AuthDtos.UserProfile(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole().name());
    }
}
