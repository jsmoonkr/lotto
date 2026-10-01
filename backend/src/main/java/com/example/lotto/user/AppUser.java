package com.example.lotto.user;

import com.example.lotto.security.crypto.EncryptedStringConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 ID. 소문자로 저장한다. */
    @Column(nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(nullable = false)
    private String passwordHash;

    // 개인정보는 암호화해서 저장한다.
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String name;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    private String email;

    /** 이메일 중복 확인용 HMAC. 소문자로 맞춘 이메일의 해시. */
    @Column(unique = true, length = 64)
    private String emailHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    private boolean systemAdmin;

    private int failedLoginCount;

    private Instant lockedUntil;

    /**
     * MFA 확장용. 지금은 항상 false이고 로그인 흐름에서 이 값을 보고 2단계 인증으로 분기할 자리만 마련해 두었다.
     * TOTP를 붙일 때 mfaSecret에 암호화된 시드를 저장한다.
     */
    private boolean mfaEnabled;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    private String mfaSecret;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastLoginAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_menu_permission", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "menu", length = 20)
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private Map<Menu, PermissionLevel> permissions = new HashMap<>();

    protected AppUser() {
    }

    public AppUser(String loginId, String passwordHash, String name, String email, String emailHash, String phone) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.email = email;
        this.emailHash = emailHash;
        this.phone = phone;
        this.createdAt = Instant.now();
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void loginFailed(int maxFailures, Instant lockUntil) {
        failedLoginCount++;
        if (failedLoginCount >= maxFailures) {
            lockedUntil = lockUntil;
            failedLoginCount = 0;
        }
    }

    public void loginSucceeded(Instant now) {
        failedLoginCount = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void unlock() {
        failedLoginCount = 0;
        lockedUntil = null;
    }

    /** 실제로 가진 권한 (SYSTEM_ADMIN이면 모든 메뉴 ADMIN). */
    public PermissionLevel levelOf(Menu menu) {
        if (systemAdmin) {
            return PermissionLevel.ADMIN;
        }
        return permissions.getOrDefault(menu, PermissionLevel.NONE);
    }

    public Map<Menu, PermissionLevel> effectivePermissions() {
        Map<Menu, PermissionLevel> result = new EnumMap<>(Menu.class);
        for (Menu menu : Menu.values()) {
            result.put(menu, levelOf(menu));
        }
        return result;
    }

    /** DB에 저장된 메뉴 권한 그대로 (SYSTEM_ADMIN 여부와 무관). */
    public Map<Menu, PermissionLevel> grantedPermissions() {
        return Collections.unmodifiableMap(permissions);
    }

    public void setPermission(Menu menu, PermissionLevel level) {
        if (level == PermissionLevel.NONE) {
            permissions.remove(menu);
        } else {
            permissions.put(menu, level);
        }
    }

    public boolean isAnyMenuAdmin() {
        return systemAdmin || permissions.containsValue(PermissionLevel.ADMIN);
    }

    public Long getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isSystemAdmin() {
        return systemAdmin;
    }

    public void setSystemAdmin(boolean systemAdmin) {
        this.systemAdmin = systemAdmin;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
