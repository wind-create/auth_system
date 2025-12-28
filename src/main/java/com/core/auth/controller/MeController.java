package com.core.auth.controller;

import com.core.auth.dto.api.ApiResponse;
import com.core.auth.dto.me.*;
import com.core.auth.security.ScopeGuard;
import com.core.auth.service.me.SelfServiceUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final SelfServiceUserService selfServiceUserService;
    private final ScopeGuard scopeGuard;

    // ============ /me/profile (GET) ============

    @GetMapping("/profile")
    public ApiResponse<SelfProfileResponse> getProfile(Authentication authentication) {
        // Wajib punya permission "profile.read_self"
        UUID userId = scopeGuard.requirePermAndUserId(authentication, "profile.read_self");

        var profile = selfServiceUserService.getMyProfile(userId);
        return ApiResponse.success(profile);
    }

    // ============ /me/profile (PUT) ============

    @PutMapping("/profile")
    public ApiResponse<SelfProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody SelfProfileUpdateRequest request
    ) {
        // Wajib punya permission "profile.update_self"
        UUID userId = scopeGuard.requirePermAndUserId(authentication, "profile.update_self");

        var profile = selfServiceUserService.updateMyProfile(userId, request);
        return ApiResponse.success(profile);
    }

    // ============ /me/password/change ============

    @PostMapping("/password/change")
    public ApiResponse<Object> changePassword(
            Authentication authentication,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        // Wajib punya permission "password.change_self"
        UUID userId = scopeGuard.requirePermAndUserId(authentication, "password.change_self");

        selfServiceUserService.changeMyPassword(userId, request);
        // bisa juga return map {"changed": true} kalau mau
        return ApiResponse.success(null);
    }

    // ============ /me/sessions (list) ============

    @GetMapping("/sessions")
    public ApiResponse<List<SessionView>> listSessions(Authentication authentication) {
        // Wajib punya permission "session.read_self"
        UUID userId = scopeGuard.requirePermAndUserId(authentication, "session.read_self");

        // optional: current session id (kalau AuthPrincipal sudah bawa sessionId)
        UUID currentSessionId = scopeGuard.currentSessionId(authentication);

        var sessions = selfServiceUserService.listMySessions(userId, currentSessionId);
        return ApiResponse.success(sessions);
    }

    // ============ /me/sessions/{sessionId}/revoke ============

    @PostMapping("/sessions/{sessionId}/revoke")
    public ApiResponse<Object> revokeSession(
            Authentication authentication,
            @PathVariable("sessionId") UUID sessionId
    ) {
        // Wajib punya permission "session.revoke_self"
        UUID userId = scopeGuard.requirePermAndUserId(authentication, "session.revoke_self");

        selfServiceUserService.revokeMySession(userId, sessionId);
        return ApiResponse.success(null);
    }
}
