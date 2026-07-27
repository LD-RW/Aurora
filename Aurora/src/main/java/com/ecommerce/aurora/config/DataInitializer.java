package com.ecommerce.aurora.config;

import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (AppRole appRole : AppRole.values()) {
            roleRepository.findByRoleName(appRole)
                    .orElseGet(() -> roleRepository.save(new Role(appRole)));
        }
    }
}
