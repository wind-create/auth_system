package com.core.auth.security;

import lombok.*;
import java.util.*;
import java.security.Principal;
import java.util.UUID;

@Getter @AllArgsConstructor
public class AuthPrincipal implements Principal {
  private final UUID userId;
  private final List<UUID> merchantIds;

  @Override public String getName() { return userId.toString(); }
}
