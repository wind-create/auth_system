package com.core.auth.repo;

import com.core.auth.entity.TotpCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TotpCredentialRepository extends JpaRepository<TotpCredential, UUID> {

  // Ambil credential aktif milik user tertentu (kalau nanti mau dipakai di login)
  Optional<TotpCredential> findByUser_IdAndActiveTrue(UUID userId);

  // Nonaktifkan semua credential aktif milik user ini (dipakai di disableForCurrentUser)
  @Modifying
  @Query("""
      update TotpCredential c
      set c.active = false
      where c.user.id = :userId and c.active = true
      """)
  int disableAllByUserId(@Param("userId") UUID userId);
}
