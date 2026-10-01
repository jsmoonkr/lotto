package com.example.lotto.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** SYSTEM_ADMIN이 한 명도 없으면 설정값(lotto.admin.*)으로 첫 관리자 계정을 만든다. */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String loginId;
    private final String name;
    private final String initialPassword;

    public AdminBootstrap(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                          @Value("${lotto.admin.login-id}") String loginId,
                          @Value("${lotto.admin.name}") String name,
                          @Value("${lotto.admin.initial-password}") String initialPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginId = loginId.toLowerCase(Locale.ROOT);
        this.name = name;
        this.initialPassword = initialPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsBySystemAdminTrue()) {
            return;
        }
        if (userRepository.existsByLoginId(loginId)) {
            log.warn("SYSTEM_ADMIN이 없지만 '{}' 아이디가 이미 일반 사용자로 있어 관리자 계정을 만들지 않았습니다.", loginId);
            return;
        }
        AppUser admin = new AppUser(loginId, passwordEncoder.encode(initialPassword), name, null, null, null);
        admin.setSystemAdmin(true);
        userRepository.save(admin);
        log.info("첫 SYSTEM_ADMIN 계정 '{}'을 만들었습니다. 로그인 후 비밀번호를 바꿔 주세요.", loginId);
    }
}
