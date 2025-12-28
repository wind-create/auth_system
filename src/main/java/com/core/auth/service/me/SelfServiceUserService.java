package com.core.auth.service.me;


import com.core.auth.dto.me.*;
import java.util.List;
import java.util.UUID;

public interface SelfServiceUserService {

    SelfProfileResponse getMyProfile(UUID userId);

    SelfProfileResponse updateMyProfile(UUID userId, SelfProfileUpdateRequest request);

    void changeMyPassword(UUID userId, PasswordChangeRequest request);

    List<SessionView> listMySessions(UUID userId, UUID currentSessionId);

    void revokeMySession(UUID userId, UUID sessionId);
}
