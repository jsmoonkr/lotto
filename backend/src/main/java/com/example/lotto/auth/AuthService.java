package com.example.lotto.auth;

import com.example.lotto.security.SecurityProperties;
import com.example.lotto.security.crypto.FieldCipher;
import com.example.lotto.user.AppUser;
import com.example.lotto.user.AppUserRepository;
import com.example.lotto.user.Menu;
import com.example.lotto.user.PermissionLevel;
import com.example.lotto.user.PersonalData;
import com.example.lotto.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String BAD_CREDENTIALS = "아이디 또는 비밀번호가 올바르지 않습니다.";

    /** 가입하면 통계만 볼 수 있다. 나머지 메뉴는 관리자가 준다. */
    private static final Menu DEFAULT_MENU = Menu.STATS;

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final FieldCipher cipher;
    private final TokenService tokenService;
    private final SecurityProperties props;
    /** 없는 아이디로 로그인해도 비밀번호 검사 시간이 같도록 쓰는 가짜 해시 */
    private final String dummyHash;

    public AuthService(AppUserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, FieldCipher cipher, TokenService tokenService,
                       SecurityProperties props) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.cipher = cipher;
        this.tokenService = tokenService;
        this.props = props;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public AppUser signup(String loginId, String password, String name, String email, String phone) {
        String normalizedId = loginId.toLowerCase(Locale.ROOT);
        String normalizedEmail = PersonalData.normalizeEmail(email);
        String emailHash = cipher.hash(normalizedEmail);
        if (userRepository.existsByLoginId(normalizedId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmailHash(emailHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        AppUser user = new AppUser(normalizedId, passwordEncoder.encode(password), name.trim(),
                normalizedEmail, emailHash, PersonalData.normalizePhone(phone));
        user.setPermission(DEFAULT_MENU, PermissionLevel.USE);
        return userRepository.save(user);
    }

    /**
     * 아이디/비밀번호 확인. 실패 횟수와 잠금 상태는 예외가 나도 저장되어야 하므로 롤백하지 않는다.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResult login(String loginId, String password) {
        Instant now = Instant.now();
        AppUser user = userRepository.findByLoginId(loginId.toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (user.isLocked(now)) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "로그인을 여러 번 실패해서 계정이 잠겼습니다. 잠시 뒤 다시 시도해 주세요.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.loginFailed(props.maxFailedLogins(), now.plus(props.lockDuration()));
            log.info("로그인 실패: {}", user.getLoginId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 계정입니다. 관리자에게 문의해 주세요.");
        }
        // MFA 확장 지점: user.isMfaEnabled()이면 여기서 토큰을 주지 않고
        // 짧게 쓰는 MFA 토큰과 함께 LoginResult.Status.MFA_REQUIRED를 돌려준 뒤, /api/auth/mfa에서 OTP를 확인해 발급한다.
        user.loginSucceeded(now);
        return issue(user, now);
    }

    /** 리프레시 토큰으로 새 액세스 토큰과 새 리프레시 토큰을 발급한다. */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResult refresh(String refreshToken) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenService.sha256(refreshToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."));
        if (stored.isRevoked()) {
            // 이미 폐기된 토큰이 다시 쓰였다면 탈취를 의심하고 그 사용자의 모든 세션을 끊는다.
            refreshTokenRepository.revokeAllByUserId(stored.getUserId(), now);
            log.warn("폐기된 리프레시 토큰 재사용 감지: userId={}", stored.getUserId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요.");
        }
        if (!stored.isUsable(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요.");
        }
        AppUser user = userRepository.findWithPermissionsById(stored.getUserId())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."));
        stored.revoke(now);
        return issue(user, now);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(TokenService.sha256(refreshToken))
                .ifPresent(t -> t.revoke(Instant.now()));
    }

    @Transactional(readOnly = true)
    public AppUser me(Long userId) {
        return userRepository.findWithPermissionsById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        AppUser user = me(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        // 다른 기기의 로그인도 끊는다.
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int deleted = refreshTokenRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("만료된 리프레시 토큰 {}개 삭제", deleted);
        }
    }

    private LoginResult issue(AppUser user, Instant now) {
        String refresh = tokenService.newRefreshToken();
        refreshTokenRepository.save(new RefreshToken(user.getId(), TokenService.sha256(refresh),
                now.plus(props.refreshTokenTtl())));
        return new LoginResult(LoginResult.Status.OK, tokenService.accessToken(user, now),
                tokenService.accessTokenTtlSeconds(), refresh, user);
    }

    /** refreshToken은 쿠키로만 내보내고 응답 본문에는 넣지 않는다. */
    public record LoginResult(Status status, String accessToken, long expiresIn, String refreshToken, AppUser user) {
        public enum Status {
            OK,
            /** MFA를 붙이면 사용한다. */
            MFA_REQUIRED
        }
    }
}
