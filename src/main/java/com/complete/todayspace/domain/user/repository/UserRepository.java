package com.complete.todayspace.domain.user.repository;

import com.complete.todayspace.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    Optional<User> findByoAuthId(Long oAuthId);

}
