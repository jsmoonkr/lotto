package com.example.lotto.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = "permissions")
    Optional<AppUser> findWithPermissionsById(Long id);

    @EntityGraph(attributePaths = "permissions")
    Optional<AppUser> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmailHash(String emailHash);

    boolean existsBySystemAdminTrue();

    @EntityGraph(attributePaths = "permissions")
    Page<AppUser> findByLoginIdContaining(String loginId, Pageable pageable);

    @EntityGraph(attributePaths = "permissions")
    Page<AppUser> findByEmailHash(String emailHash, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "permissions")
    Page<AppUser> findAll(Pageable pageable);
}
