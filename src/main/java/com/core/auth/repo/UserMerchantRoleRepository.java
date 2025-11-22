package com.core.auth.repo;

import com.core.auth.entity.UserMerchantRole;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface UserMerchantRoleRepository extends JpaRepository<UserMerchantRole, UserMerchantRole.PK> {

  @Query("""
    select distinct umr.merchantId
    from UserMerchantRole umr
    where umr.userId = :uid
      and (umr.expiresAt is null or umr.expiresAt > CURRENT_TIMESTAMP)
  """)
  List<java.util.UUID> findActiveMerchantIdsOfUser(@Param("uid") java.util.UUID userId);

  @Query("""
    select distinct p.code
    from Permission p
      join RolePermission rp on rp.permId = p.id
      join UserMerchantRole umr on umr.roleId = rp.roleId
    where umr.userId = :uid
      and (umr.expiresAt is null or umr.expiresAt > CURRENT_TIMESTAMP)
  """)
  List<String> findOrgPermCodesOfUser(@Param("uid") java.util.UUID userId);
}
