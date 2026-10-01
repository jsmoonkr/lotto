package com.example.lotto.security;

import com.example.lotto.security.crypto.FieldCipher;
import com.example.lotto.user.Menu;
import com.example.lotto.user.PermissionLevel;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static com.example.lotto.security.UserJwtAuthenticationConverter.SYSTEM_ADMIN;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            UserJwtAuthenticationConverter jwtConverter) throws Exception {
        String[] menuAdmins = new String[Menu.values().length + 1];
        menuAdmins[0] = SYSTEM_ADMIN;
        for (Menu m : Menu.values()) {
            menuAdmins[m.ordinal() + 1] = m.authority(PermissionLevel.ADMIN);
        }

        http
                // 액세스 토큰은 Authorization 헤더로만 받으므로 CSRF 토큰이 필요 없다.
                // 리프레시 토큰 쿠키는 SameSite=Strict, Path=/api/auth로 묶어 다른 사이트에서 보낼 수 없게 했다.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/refresh",
                                "/api/auth/logout").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        // 최신 회차는 추천 화면에서도 쓰는 공개 정보라 로그인만 되어 있으면 볼 수 있다.
                        .requestMatchers(HttpMethod.GET, "/api/draws/latest").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/draws/sync").hasAuthority(adminOf(Menu.DRAWS))
                        .requestMatchers("/api/draws/**").hasAuthority(useOf(Menu.DRAWS))
                        .requestMatchers("/api/stats/**").hasAuthority(useOf(Menu.STATS))
                        .requestMatchers(HttpMethod.POST, "/api/recommendations").hasAuthority(useOf(Menu.RECOMMEND))
                        .requestMatchers(HttpMethod.GET, "/api/recommendations").hasAuthority(useOf(Menu.HISTORY))
                        // 사용자 목록과 권한 변경은 메뉴 ADMIN도 쓴다. 메뉴별 세부 검사는 AdminUserService에서 한다.
                        .requestMatchers(HttpMethod.GET, "/api/admin/users").hasAnyAuthority(menuAdmins)
                        .requestMatchers(HttpMethod.PUT, "/api/admin/users/*/permissions").hasAnyAuthority(menuAdmins)
                        .requestMatchers("/api/admin/**").hasAuthority(SYSTEM_ADMIN)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }

    private static String useOf(Menu menu) {
        return menu.authority(PermissionLevel.USE);
    }

    private static String adminOf(Menu menu) {
        return menu.authority(PermissionLevel.ADMIN);
    }

    /** 기본은 BCrypt. 나중에 알고리즘을 바꿔도 기존 해시({bcrypt}...)를 그대로 검증할 수 있다. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    FieldCipher fieldCipher(SecurityProperties props) {
        return new FieldCipher(props.aesKey(), props.hmacKey());
    }

    @Bean
    SecretKey jwtKey(SecurityProperties props) {
        byte[] key = Base64.getDecoder().decode(props.jwtSecret());
        if (key.length < 32) {
            throw new IllegalStateException("JWT 키는 32바이트 이상이어야 합니다.");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
