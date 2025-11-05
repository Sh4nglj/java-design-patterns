package com.iluwatar.model.repository;

import com.iluwatar.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.deleted = false AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchByUsernameOrEmail(@Param("keyword") String keyword, Pageable pageable);

    Optional<User> findByIdAndDeletedFalse(Long id);
}