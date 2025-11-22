package com.core.auth.repo;
import com.core.auth.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MerchantRepository extends JpaRepository<Merchant, java.util.UUID> {
  Optional<Merchant> findByCode(String code);
}
