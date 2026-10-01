package com.example.lotto.admin;

import com.example.lotto.auth.RefreshTokenRepository;
import com.example.lotto.security.crypto.FieldCipher;
import com.example.lotto.user.AppUser;
import com.example.lotto.user.AppUserRepository;
import com.example.lotto.user.Menu;
import com.example.lotto.user.PermissionLevel;
import com.example.lotto.user.PersonalData;
import com.example.lotto.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * 사용자 관리. 호출하는 관리자(actor)의 권한을 여기서 한 번 더 확인한다.
 * <ul>
 *   <li>SYSTEM_ADMIN: 모든 작업</li>
 *   <li>메뉴 ADMIN: 사용자 목록 조회(이메일/휴대폰은 가려서), 자기가 ADMIN인 메뉴의 USE 권한 부여/회수</li>
 * </ul>
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FieldCipher cipher;

    public AdminUserService(AppUserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                            FieldCipher cipher) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cipher = cipher;
    }

    /** q가 @를 포함하면 이메일 정확히 일치, 아니면 아이디 부분 일치로 찾는다. */
    @Transactional(readOnly = true)
    public Page<AdminUserView> list(Long actorId, String q, int page, int size) {
        AppUser actor = load(actorId);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<AppUser> users;
        if (q == null || q.isBlank()) {
            users = userRepository.findAll(pageable);
        } else if (q.contains("@")) {
            users = userRepository.findByEmailHash(cipher.hash(PersonalData.normalizeEmail(q)), pageable);
        } else {
            users = userRepository.findByLoginIdContaining(q.trim().toLowerCase(Locale.ROOT), pageable);
        }
        boolean fullView = actor.isSystemAdmin();
        Instant now = Instant.now();
        return users.map(u -> AdminUserView.of(u, fullView, now));
    }

    @Transactional
    public AdminUserView setPermission(Long actorId, Long userId, Menu menu, PermissionLevel level) {
        AppUser actor = load(actorId);
        AppUser target = load(userId);
        if (!actor.isSystemAdmin()) {
            if (actor.levelOf(menu) != PermissionLevel.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, menu.label() + " 메뉴의 관리자만 바꿀 수 있습니다.");
            }
            if (level == PermissionLevel.ADMIN || target.grantedPermissions().get(menu) == PermissionLevel.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "메뉴 관리자 지정과 해제는 시스템 관리자만 할 수 있습니다.");
            }
            if (target.isSystemAdmin()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "시스템 관리자의 권한은 바꿀 수 없습니다.");
            }
        }
        target.setPermission(menu, level);
        log.info("권한 변경: {} → {} {}={}", actor.getLoginId(), target.getLoginId(), menu, level);
        return AdminUserView.of(target, actor.isSystemAdmin(), Instant.now());
    }

    @Transactional
    public AdminUserView setStatus(Long actorId, Long userId, UserStatus status) {
        AppUser target = load(userId);
        if (actorId.equals(userId) && status != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 계정은 정지할 수 없습니다.");
        }
        target.setStatus(status);
        if (status != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        }
        log.info("상태 변경: {} → {}", target.getLoginId(), status);
        return AdminUserView.of(target, true, Instant.now());
    }

    @Transactional
    public AdminUserView unlock(Long userId) {
        AppUser target = load(userId);
        target.unlock();
        return AdminUserView.of(target, true, Instant.now());
    }

    @Transactional
    public AdminUserView setSystemAdmin(Long actorId, Long userId, boolean value) {
        if (actorId.equals(userId) && !value) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신의 시스템 관리자 권한은 해제할 수 없습니다.");
        }
        AppUser target = load(userId);
        target.setSystemAdmin(value);
        log.info("시스템 관리자 {}: {}", value ? "지정" : "해제", target.getLoginId());
        return AdminUserView.of(target, true, Instant.now());
    }

    private AppUser load(Long id) {
        return userRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    /**
     * fullView가 false(메뉴 관리자)면 이메일과 휴대폰을 가린다.
     * permissions는 DB에 저장된 메뉴 권한이고, systemAdmin이면 실제로는 모든 메뉴 ADMIN이다.
     */
    public record AdminUserView(Long id, String loginId, String name, String email, String phone,
                                UserStatus status, boolean systemAdmin, boolean locked,
                                Map<Menu, PermissionLevel> permissions, Instant createdAt, Instant lastLoginAt) {

        static AdminUserView of(AppUser u, boolean fullView, Instant now) {
            String email = fullView ? u.getEmail() : PersonalData.maskEmail(u.getEmail());
            String phone = fullView ? PersonalData.formatPhone(u.getPhone()) : PersonalData.maskPhone(u.getPhone());
            return new AdminUserView(u.getId(), u.getLoginId(), u.getName(), email, phone, u.getStatus(),
                    u.isSystemAdmin(), u.isLocked(now), Map.copyOf(u.grantedPermissions()),
                    u.getCreatedAt(), u.getLastLoginAt());
        }
    }
}
