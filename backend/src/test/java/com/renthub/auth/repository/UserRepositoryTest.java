package com.renthub.auth.repository;

import com.renthub.auth.model.entity.User;
import com.renthub.common.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindByEmail() {
        User user = User.builder()
                .email("testrepo@renthub.com")
                .passwordHash("hashed")
                .firstName("Test")
                .lastName("Repo")
                .isVerified(true)
                .isOwner(false)
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("testrepo@renthub.com");
        assertTrue(found.isPresent());
        assertEquals("Test", found.get().getFirstName());
        assertTrue(userRepository.existsByEmail("testrepo@renthub.com"));
    }
}
