package com.ddib.monolith.auth.infra;

import com.ddib.monolith.auth.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findTopByOrderByUserIdDesc();
}

