package com.example.lotto.user;

import com.example.lotto.security.UserJwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserTest {

    private AppUser user() {
        return new AppUser("hong", "hash", "홍길동", "hong@example.com", "h", "01012345678");
    }

    @Test
    void locksAfterMaxFailuresAndUnlocksAfterSuccess() {
        AppUser u = user();
        Instant now = Instant.now();
        Instant until = now.plusSeconds(900);
        for (int i = 0; i < 4; i++) {
            u.loginFailed(5, until);
        }
        assertThat(u.isLocked(now)).isFalse();
        u.loginFailed(5, until);
        assertThat(u.isLocked(now)).isTrue();
        assertThat(u.isLocked(until.plusSeconds(1))).isFalse();
        u.loginSucceeded(now);
        assertThat(u.isLocked(now)).isFalse();
    }

    @Test
    void permissionsAndAuthorities() {
        AppUser u = user();
        u.setPermission(Menu.STATS, PermissionLevel.USE);
        u.setPermission(Menu.DRAWS, PermissionLevel.ADMIN);
        assertThat(u.levelOf(Menu.RECOMMEND)).isEqualTo(PermissionLevel.NONE);
        assertThat(authorities(u)).containsExactlyInAnyOrder(
                "MENU_DRAWS_USE", "MENU_DRAWS_ADMIN", "MENU_STATS_USE");

        u.setPermission(Menu.DRAWS, PermissionLevel.NONE);
        assertThat(u.grantedPermissions()).containsOnlyKeys(Menu.STATS);
        assertThat(u.isAnyMenuAdmin()).isFalse();
    }

    @Test
    void systemAdminGetsEverything() {
        AppUser u = user();
        u.setSystemAdmin(true);
        assertThat(u.effectivePermissions()).allSatisfy((m, l) -> assertThat(l).isEqualTo(PermissionLevel.ADMIN));
        assertThat(authorities(u)).contains("SYSTEM_ADMIN", "MENU_RECOMMEND_ADMIN", "MENU_HISTORY_USE");
    }

    @Test
    void masking() {
        assertThat(PersonalData.maskEmail("hong@example.com")).isEqualTo("ho**@example.com");
        assertThat(PersonalData.maskPhone("01012345678")).isEqualTo("010-****-5678");
        assertThat(PersonalData.formatPhone(PersonalData.normalizePhone("010-1234-5678"))).isEqualTo("010-1234-5678");
        assertThat(PersonalData.normalizeEmail(" Hong@Example.COM ")).isEqualTo("hong@example.com");
    }

    private static java.util.List<String> authorities(AppUser u) {
        return UserJwtAuthenticationConverter.authoritiesOf(u).stream().map(GrantedAuthority::getAuthority).toList();
    }
}
