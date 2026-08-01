package com.ecommerce.aurora.config;

import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.repositories.RoleRepository;
import com.ecommerce.aurora.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.app.admin.username}")
    private String adminUsername;
    @Value("${spring.app.admin.password}")
    private String adminPassword;
    @Value("${spring.app.admin.email}")
    private String adminEmail;

    @Override
    @Transactional
    public void run(String... args) {
        Map<AppRole, Role> roles = new EnumMap<>(AppRole.class);
        for (AppRole appRole : AppRole.values()) {
            Role role = roleRepository.findByRoleName(appRole)
                    .orElseGet(() -> roleRepository.save(new Role(appRole)));
            roles.put(appRole, role);
        }

        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = new User(adminUsername, passwordEncoder.encode(adminPassword), adminEmail);
            admin.setRoles(Set.of(roles.get(AppRole.ROLE_ADMIN)));
            userRepository.save(admin);
        }
    }
}
