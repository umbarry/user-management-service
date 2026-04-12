package com.umbarry.usermanagementservice.repository;

import com.umbarry.usermanagementservice.enumeration.UserStatus;
import com.umbarry.usermanagementservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByTaxCode(String taxCode);
    Page<User> findByStatusIn(Collection<UserStatus> statuses, Pageable pageable);
}
