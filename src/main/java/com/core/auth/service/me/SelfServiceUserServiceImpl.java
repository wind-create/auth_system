package com.core.auth.service.me;

// package com.mestika.auth.service.me;

import com.core.auth.dto.me.*;
import com.core.auth.entity.SessionEntity;
import com.core.auth.entity.SessionStatus;
import com.core.auth.entity.UserAccount;
import com.core.auth.repo.SessionRepository;
import com.core.auth.repo.UserAccountRepository;
import com.core.auth.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SelfServiceUserServiceImpl implements SelfServiceUserService {

    private final UserAccountRepository userAccountRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public SelfProfileResponse getMyProfile(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("user_not_found", "User tidak ditemukan"));

        return SelfProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                // kalau ada di entity kamu:
                // .emailVerified(user.getEmailVerified())
                .createdAt(toOffset(user.getCreatedAt()))
                // kalau kamu punya last_login_at di entity, map di sini:
                // .lastLoginAt(toOffset(user.getLastLoginAt()))
                .build();
    }

    @Override
    public SelfProfileResponse updateMyProfile(UUID userId, SelfProfileUpdateRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("user_not_found", "User tidak ditemukan"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        userAccountRepository.save(user);

        return getMyProfile(userId);
    }

    @Override
    public void changeMyPassword(UUID userId, PasswordChangeRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("user_not_found", "User tidak ditemukan"));

        // cek password lama
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("invalid_password", "Password lama salah");
        }

        // TODO: bisa tambahkan policy (min length, complexity, dll)
        String encoded = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(encoded);

        userAccountRepository.save(user);

        // Optional tapi bagus:
        // - bump ASV / revoke semua session lain
        // - atau call service SessionService.logoutAll(userId)
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionView> listMySessions(UUID userId, UUID currentSessionId) {
        List<SessionEntity> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return sessions.stream()
                .map(s -> SessionView.builder()
                        .sessionId(s.getId())
                        .createdAt(toOffset(s.getCreatedAt()))
                        .lastSeenAt(toOffset(s.getLastRotatedAt()))
                        .ipAddress(s.getIpAddress())
                        .userAgent(s.getUserAgent())
                        .status(s.getStatus().name())
                        .current(s.getId().equals(currentSessionId))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void revokeMySession(UUID userId, UUID sessionId) {
        SessionEntity session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> ApiException.notFound("session_not_found", "Session tidak ditemukan"));

        // jangan allow revoke session yang sudah revoked/expired (optional)
        // if (session.getStatus() != SessionStatus.ACTIVE) ...

        session.setStatus(SessionStatus.revoked); // atau setStatus(REVOKED), setRevokedAt(now) dsb.
        sessionRepository.save(session);
    }

    private OffsetDateTime toOffset(java.time.temporal.TemporalAccessor t) {
        if (t == null) return null;
        if (t instanceof OffsetDateTime odt) return odt;
        if (t instanceof java.time.LocalDateTime ldt) {
            return ldt.atOffset(java.time.ZoneOffset.UTC);
        }
        if (t instanceof java.time.Instant instant) {
            return instant.atOffset(java.time.ZoneOffset.UTC);
        }
        return null;
    }
}
