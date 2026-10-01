package com.example.lotto.security;

import com.example.lotto.user.AppUser;
import com.example.lotto.user.AppUserRepository;
import com.example.lotto.user.Menu;
import com.example.lotto.user.PermissionLevel;
import com.example.lotto.user.UserStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT에는 사용자 ID만 담고, 권한은 요청마다 DB에서 읽는다.
 * 그래서 권한을 바꾸거나 계정을 정지하면 토큰이 만료되기를 기다리지 않고 바로 반영된다.
 */
@Component
public class UserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

    private final AppUserRepository userRepository;

    public UserJwtAuthenticationConverter(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AppUser user = userRepository.findWithPermissionsById(Long.valueOf(jwt.getSubject()))
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new DisabledException("사용할 수 없는 계정입니다."));
        return new JwtAuthenticationToken(jwt, authoritiesOf(user), user.getLoginId());
    }

    public static List<GrantedAuthority> authoritiesOf(AppUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.isSystemAdmin()) {
            authorities.add(new SimpleGrantedAuthority(SYSTEM_ADMIN));
        }
        for (Menu menu : Menu.values()) {
            PermissionLevel level = user.levelOf(menu);
            if (level == PermissionLevel.USE || level == PermissionLevel.ADMIN) {
                authorities.add(new SimpleGrantedAuthority(menu.authority(PermissionLevel.USE)));
            }
            if (level == PermissionLevel.ADMIN) {
                authorities.add(new SimpleGrantedAuthority(menu.authority(PermissionLevel.ADMIN)));
            }
        }
        return authorities;
    }
}
