package com.core.auth.repo;

import com.core.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailNormalized(String emailNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    @Query("select u.authStateVersion from UserAccount u where u.id = :id")
    Integer findAuthStateVersionById(@Param("id") UUID id);
    
    @Modifying
    @Query("update UserAccount u set u.authStateVersion = u.authStateVersion + 1, u.authStateChangedAt = CURRENT_TIMESTAMP where u.id = :id")
    void bumpAuthStateVersion(@Param("id") UUID id);


    
}
