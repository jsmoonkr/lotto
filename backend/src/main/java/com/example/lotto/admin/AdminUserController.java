package com.example.lotto.admin;

import com.example.lotto.admin.AdminUserService.AdminUserView;
import com.example.lotto.user.Menu;
import com.example.lotto.user.PermissionLevel;
import com.example.lotto.user.UserStatus;
import com.example.lotto.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@Validated
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AdminUserView> list(@AuthenticationPrincipal Jwt jwt,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.of(service.list(actor(jwt), q, page, size));
    }

    @PutMapping("/{id}/permissions")
    public AdminUserView setPermission(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                       @Valid @RequestBody PermissionRequest req) {
        return service.setPermission(actor(jwt), id, req.menu(), req.level());
    }

    @PutMapping("/{id}/status")
    public AdminUserView setStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                   @Valid @RequestBody StatusRequest req) {
        return service.setStatus(actor(jwt), id, req.status());
    }

    @PostMapping("/{id}/unlock")
    public AdminUserView unlock(@PathVariable Long id) {
        return service.unlock(id);
    }

    @PutMapping("/{id}/system-admin")
    public AdminUserView setSystemAdmin(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                        @Valid @RequestBody SystemAdminRequest req) {
        return service.setSystemAdmin(actor(jwt), id, req.value());
    }

    private static Long actor(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public record PermissionRequest(@NotNull Menu menu, @NotNull PermissionLevel level) {
    }

    public record StatusRequest(@NotNull UserStatus status) {
    }

    public record SystemAdminRequest(@NotNull Boolean value) {
    }
}
