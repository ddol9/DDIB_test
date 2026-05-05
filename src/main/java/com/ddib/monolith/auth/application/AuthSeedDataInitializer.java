package com.ddib.monolith.auth.application;

import com.ddib.monolith.auth.domain.Credential;
import com.ddib.monolith.auth.domain.Role;
import com.ddib.monolith.auth.domain.User;
import com.ddib.monolith.auth.infra.CredentialRepository;
import com.ddib.monolith.auth.infra.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class AuthSeedDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        User user = userRepository.save(User.builder()
                .name("Monolith Tester")
                .phoneNumber("010-1234-5678")
                .role(Role.USER)
                .build());

        credentialRepository.save(Credential.builder()
                .user(user)
                .provider("TEST")
                .providerUid("seed-user-1")
                .build());
    }
}

