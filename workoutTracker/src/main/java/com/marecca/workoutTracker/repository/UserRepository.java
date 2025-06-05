package com.marecca.workoutTracker.repository;

import com.marecca.workoutTracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsById(Long userId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}