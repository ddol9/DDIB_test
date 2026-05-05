package com.ddib.monolith.auth.application;

import com.ddib.monolith.auth.api.dto.LoginResponse;
import com.ddib.monolith.auth.api.dto.UserResponse;
import com.ddib.monolith.auth.domain.Credential;
import com.ddib.monolith.auth.domain.Role;
import com.ddib.monolith.auth.domain.User;
import com.ddib.monolith.auth.domain.UserSnapshot;
import com.ddib.monolith.auth.exception.AuthErrorCode;
import com.ddib.monolith.auth.infra.CredentialRepository;
import com.ddib.monolith.auth.infra.UserRepository;
import com.ddib.monolith.support.exception.CustomException;
import com.ddib.monolith.support.security.JwtTokenProvider;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthTestService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public List<Long> createDummyUsers() {
        List<Long> createdUserIds = new ArrayList<>();
        long startIndex = userRepository.findTopByOrderByUserIdDesc()
                .map(User::getUserId)
                .orElse(0L) + 1L;

        for (long i = startIndex; i < startIndex + 10; i++) {
            String providerUid = "test_uid_" + i;
            Credential existing = credentialRepository.findByProviderAndProviderUid("TEST", providerUid).orElse(null);
            if (existing != null) {
                createdUserIds.add(existing.getUser().getUserId());
                continue;
            }

            User user = userRepository.save(User.builder()
                    .name("Dummy User " + i)
                    .phoneNumber(String.format("010-0000-%04d", i))
                    .role(Role.USER)
                    .build());
            credentialRepository.save(Credential.builder()
                    .user(user)
                    .provider("TEST")
                    .providerUid(providerUid)
                    .build());
            createdUserIds.add(user.getUserId());
        }
        return createdUserIds;
    }

    @Transactional
    public LoginResponse loginTestUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        UserSnapshot snapshot = UserSnapshot.from(user);
        String accessToken = jwtTokenProvider.generateAccessToken(snapshot.userId(), snapshot.name(), snapshot.role().name());
        return LoginResponse.of(accessToken, "TEST-REFRESH-TOKEN", UserResponse.from(snapshot));
    }
}

