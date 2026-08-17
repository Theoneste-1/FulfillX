package com.fulfillx.auth.config;

import com.fulfillx.auth.domain.UserAccount;
import com.fulfillx.auth.infrastructure.persistence.UserAccountRepository;
import com.fulfillx.common.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DemoUserSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("admin@fulfillx.com", "Admin123!", "FulfillX Admin", Set.of(Roles.ADMIN));
        seed("warehouse@fulfillx.com", "Operator123!", "Warehouse Operator", Set.of(Roles.WAREHOUSE_OPERATOR));
        seed("logistics@fulfillx.com", "Operator123!", "Logistics Operator", Set.of(Roles.LOGISTICS_OPERATOR));
        seed("support@fulfillx.com", "Support123!", "Support Agent", Set.of(Roles.SUPPORT));
        seed("customer@fulfillx.com", "Customer123!", "Ada Customer", Set.of(Roles.CUSTOMER));
    }

    private void seed(String email, String password, String name, Set<String> roles) {
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        users.save(UserAccount.create(email, passwordEncoder.encode(password), name, roles));
        log.info("Seeded user {}", email);
    }
}
