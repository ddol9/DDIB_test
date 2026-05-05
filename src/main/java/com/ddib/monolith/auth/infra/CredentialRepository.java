package com.ddib.monolith.auth.infra;

import com.ddib.monolith.auth.domain.Credential;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByProviderAndProviderUid(String provider, String providerUid);
}

