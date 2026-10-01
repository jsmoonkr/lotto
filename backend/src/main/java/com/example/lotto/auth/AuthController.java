package com.example.lotto.auth;

import com.example.lotto.auth.AuthService.LoginResult;
import com.example.lotto.security.SecurityProperties;
import com.example.lotto.user.AppUser;
import com.example.lotto.user.Menu;
import com.example.lotto.user.PermissionLevel;
import com.example.lotto.user.PersonalData;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    static final String PASSWORD_MESSAGE = "비밀번호는 영문과 숫자를 포함해 8자 이상이어야 합니다.";

    private final AuthService authService;
    private final SecurityProperties props;

    public AuthController(AuthService authService, SecurityProperties props) {
        this.authService = authService;
        this.props = props;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MeView signup(@Valid @RequestBody SignupRequest req) {
        return MeView.of(authService.signup(req.loginId(), req.password(), req.name(), req.email(), req.phone()));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        return respond(authService.login(req.loginId(), req.password()), response);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                 HttpServletResponse response) {
        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        try {
            return respond(authService.refresh(refreshToken), response);
        } catch (ResponseStatusException e) {
            clearCookie(response);
            throw e;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                       HttpServletResponse response) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        clearCookie(response);
    }

    @GetMapping("/me")
    public MeView me(@AuthenticationPrincipal Jwt jwt) {
        return MeView.of(authService.me(Long.valueOf(jwt.getSubject())));
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest req,
                               HttpServletResponse response) {
        authService.changePassword(Long.valueOf(jwt.getSubject()), req.currentPassword(), req.newPassword());
        clearCookie(response);
    }

    private LoginResponse respond(LoginResult result, HttpServletResponse response) {
        setCookie(response, result.refreshToken(), props.refreshTokenTtl());
        return new LoginResponse(result.status(), result.accessToken(), result.expiresIn(), MeView.of(result.user()));
    }

    private void clearCookie(HttpServletResponse response) {
        setCookie(response, "", Duration.ZERO);
    }

    private void setCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(props.secureCookie())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record SignupRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "아이디는 영문, 숫자, _ 로 4~20자여야 합니다.")
            String loginId,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE)
            String password,
            @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
            String name,
            @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 형식이 올바르지 않습니다.") @Size(max = 100)
            String email,
            @Pattern(regexp = "^$|^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
            String phone) {
    }

    public record LoginRequest(@NotBlank String loginId, @NotBlank String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String newPassword) {
    }

    public record LoginResponse(LoginResult.Status status, String accessToken, long expiresIn, MeView user) {
    }

    public record MeView(Long id, String loginId, String name, String email, String phone,
                         boolean systemAdmin, Map<Menu, PermissionLevel> permissions) {

        public static MeView of(AppUser u) {
            return new MeView(u.getId(), u.getLoginId(), u.getName(), u.getEmail(),
                    PersonalData.formatPhone(u.getPhone()), u.isSystemAdmin(), u.effectivePermissions());
        }
    }
}
