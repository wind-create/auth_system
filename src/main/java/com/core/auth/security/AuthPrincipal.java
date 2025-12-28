package com.core.auth.security;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuthPrincipal implements Principal {
  private final UUID userId;
  private final UUID sessionId;          // ⬅️ baru
  private final List<UUID> merchantIds;

  @Override
  public String getName() {
    return userId != null ? userId.toString() : "";
  }
}
