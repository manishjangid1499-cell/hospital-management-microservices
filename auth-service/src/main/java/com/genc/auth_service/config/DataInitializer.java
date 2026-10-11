package com.genc.auth_service.config;

import com.genc.auth_service.model.Role;
import com.genc.auth_service.model.User;
import com.genc.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.seed-demo-data",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create default admin user if not present (never overwrite existing credentials)
        if (!userRepository.existsByEmail("admin@hospital.com")) {
            User admin = User.builder()
                    .email("admin@hospital.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Admin")
                    .phone("1234567890")
                    .role(Role.ADMIN)
                    .active(true)
                    .isFirstLogin(false)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created: admin@hospital.com / admin123");
        } else {
            log.info("Admin user already exists; preserving existing credentials.");
        }

        // Create sample doctor
        if (!userRepository.existsByEmail("doctor1@hospital.com")) {
            User doctor = User.builder()
                    .email("doctor1@hospital.com")
                    .password(passwordEncoder.encode("doctorRavi123"))
                    .fullName("Ravi Kumar")
                    .phone("1234567897")
                    .role(Role.DOCTOR)
                    .active(true)
                    .isFirstLogin(false)
                    .build();
            userRepository.save(doctor);
            log.info("Sample doctor created: doctor1@hospital.com / doctorRavi123");
        }

        // Create sample doctor 2
        if (!userRepository.existsByEmail("doctor2@hospital.com")) {
            User doctor = User.builder()
                    .email("doctor2@hospital.com")
                    .password(passwordEncoder.encode("doctorSuraj123"))
                    .fullName("Suraj Jana")
                    .phone("1234567791")
                    .role(Role.DOCTOR)
                    .active(true)
                    .isFirstLogin(false)
                    .build();
            userRepository.save(doctor);
            log.info("Sample doctor created: doctor2@hospital.com / doctorSuraj123");
        }

        // Create sample doctor 3
        if (!userRepository.existsByEmail("doctor3@hospital.com")) {
            User doctor = User.builder()
                    .email("doctor3@hospital.com")
                    .password(passwordEncoder.encode("doctorVishal123"))
                    .fullName("Vishal Singh")
                    .phone("1234537891")
                    .role(Role.DOCTOR)
                    .active(true)
                    .isFirstLogin(false)
                    .build();
            userRepository.save(doctor);
            log.info("Sample doctor created: doctor3@hospital.com / doctorVishal123");
        }

        // Create sample receptionist
        if (!userRepository.existsByEmail("receptionist@hospital.com")) {
            User receptionist = User.builder()
                    .email("receptionist@hospital.com")
                    .password(passwordEncoder.encode("reception123"))
                    .fullName("Jon Doe")
                    .phone("1234567892")
                    .role(Role.RECEPTIONIST)
                    .active(true)
                    .isFirstLogin(false)
                    .build();
            userRepository.save(receptionist);
            log.info("Sample receptionist created: receptionist@hospital.com / reception123");
        }

        // Create sample pharmacist
        if (!userRepository.existsByEmail("pharmacist@hospital.com")) {
            User pharmacist = User.builder()
                    .email("pharmacist@hospital.com")
                    .password(passwordEncoder.encode("pharma123"))
                    .fullName("Mike Johnson")
                    .phone("1234567893")
                    .role(Role.PHARMACIST)
                    .active(true)
                    .isFirstLogin(false)
                    .build();
            userRepository.save(pharmacist);
            log.info("Sample pharmacist created: pharmacist@hospital.com / pharma123");
        }

        // Create sample billing officer
        if (!userRepository.existsByEmail("billing@hospital.com")) {
            User billingOfficer = User.builder()
                    .email("billing@hospital.com")
                    .password(passwordEncoder.encode("billing123"))
                    .fullName("Sarah Williams")
                    .phone("1234567894")
                    .role(Role.BILLING_OFFICER)
                    .active(true)
                    .isFirstLogin(false)
                    .build();
            userRepository.save(billingOfficer);
            log.info("Sample billing officer created: billing@hospital.com / billing123");
        }

        log.info("==============================================");
        log.info("DEFAULT USERS INITIALIZED:");
        log.info("Admin:        admin@hospital.com / admin123");
        log.info("Doctor 1:       doctor1@hospital.com / doctorRavi123");
        log.info("Doctor 2:       doctor2@hospital.com / doctorSuraj123");
        log.info("Doctor 3:       doctor3@hospital.com / doctorVishal123");
        log.info("Receptionist: receptionist@hospital.com / reception123");
        log.info("Pharmacist:   pharmacist@hospital.com / pharma123");
        log.info("Billing:      billing@hospital.com / billing123");
        log.info("==============================================");
    }
}

