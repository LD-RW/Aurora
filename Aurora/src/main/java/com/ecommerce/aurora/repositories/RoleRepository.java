package com.ecommerce.aurora.repositories;

import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(AppRole roleName);
}
