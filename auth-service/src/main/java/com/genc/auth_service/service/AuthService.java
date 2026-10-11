package com.genc.auth_service.service;

import com.genc.auth_service.dto.*;
import com.genc.auth_service.exception.DuplicateResourceException;
import com.genc.auth_service.exception.ResourceNotFoundException;
import com.genc.auth_service.model.Role;
import com.genc.auth_service.model.User;
import com.genc.auth_service.repository.UserRepository;
import com.genc.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final com.genc.auth_service.client.AppointmentServiceClient appointmentServiceClient;
    private final com.genc.auth_service.client.EhrServiceClient ehrServiceClient;
    private final com.genc.auth_service.client.PharmacyServiceClient pharmacyServiceClient;
    private final com.genc.auth_service.client.PatientServiceClient patientServiceClient;

    // Login user and return JWT token
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (!user.getActive()) {
            throw new RuntimeException("User account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isFirstLogin(user.getIsFirstLogin() != null ? user.getIsFirstLogin() : false)
                .build();
    }

    // Register new user (for patient self-registration)
    public AuthResponse register(RegisterRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank() || request.getPassword().length() < 6) {
            throw new RuntimeException("Password is required and must be at least 6 characters");
        }

        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new DuplicateResourceException("Email already registered");
        }

        if (request.getPhone() != null && !request.getPhone().isEmpty() 
                && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.PATIENT)  // Self-registration is always PATIENT
                .active(true)
                .isFirstLogin(false) // Self-registered patients create their own password -> isFirstLogin = false
                .build();

        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isFirstLogin(false)
                .build();
    }

    // Admin or Receptionist creates a new user with specific role (generates temporary password)
    public UserDTO createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new DuplicateResourceException("Email already registered");
        }

        if (request.getPhone() != null && !request.getPhone().isEmpty() 
                && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        if (request.getRole() == null) {
            throw new RuntimeException("Role is required for user creation");
        }

        // Generate temporary password if not explicitly supplied or if registered by Receptionist/Admin
        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : com.genc.auth_service.util.PasswordGenerator.generateTemporaryPassword();

        boolean isFirstLogin = true;

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(rawPassword))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .active(true)
                .isFirstLogin(isFirstLogin)
                .build();

        user = userRepository.save(user);

        // Send Email notification containing the temporary password
        boolean emailSent = emailService.sendTemporaryPasswordEmail(user.getEmail(), user.getFullName(), rawPassword, false);

        UserDTO dto = mapToDTO(user);
        dto.setTempPassword(rawPassword);
        dto.setEmailSent(emailSent);
        return dto;
    }

    // Get all users
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Get user by ID
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToDTO(user);
    }

    // Get users by role
    public List<UserDTO> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Deactivate user
    public UserDTO deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(false);
        user = userRepository.save(user);
        return mapToDTO(user);
    }

    // Activate user
    public UserDTO activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(true);
        user = userRepository.save(user);
        return mapToDTO(user);
    }

    // Reset password for target user by Admin (requires Admin current password authentication)
    public UserDTO resetPassword(Long targetUserId, Long adminId, AdminResetPasswordRequest request) {
        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with id: " + adminId));

        if (!passwordEncoder.matches(request.getAdminCurrentPassword(), adminUser.getPassword())) {
            throw new RuntimeException("Admin current password is invalid");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with id: " + targetUserId));

        if (passwordEncoder.matches(request.getNewPassword(), targetUser.getPassword())) {
            throw new RuntimeException("New password cannot be the same as the user's current password");
        }

        targetUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        targetUser.setIsFirstLogin(true); // Force target user to change password on next login
        targetUser = userRepository.save(targetUser);

        log.info("Admin ID {} reset password for User ID {}", adminId, targetUserId);
        return mapToDTO(targetUser);
    }

    // Delete user (admin) - permanently removes user
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Perform cascade deletion based on role
        if (user.getRole() == Role.DOCTOR) {
            try {
                appointmentServiceClient.deleteByDoctorId(id);
            } catch (Exception e) {
                log.warn("Failed to delete doctor appointments: {}", e.getMessage());
            }
            try {
                ehrServiceClient.deleteByDoctorId(id);
            } catch (Exception e) {
                log.warn("Failed to delete doctor clinical records: {}", e.getMessage());
            }
            try {
                pharmacyServiceClient.deleteByDoctorId(id);
            } catch (Exception e) {
                log.warn("Failed to delete doctor prescriptions: {}", e.getMessage());
            }
        } else if (user.getRole() == Role.PATIENT) {
            try {
                patientServiceClient.deletePatientByUserId(id);
            } catch (Exception e) {
                log.warn("Failed to delete patient records: {}", e.getMessage());
            }
        }

        userRepository.delete(user);
    }

    // Change Password (for first login password change or voluntary password change)
    public UserDTO changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is invalid");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password cannot be the same as the current/temporary password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setIsFirstLogin(false);
        user = userRepository.save(user);

        log.info("Password updated successfully for user ID: {}", userId);
        return mapToDTO(user);
    }

    // Forgot Password (generates new temporary password)
    public UserDTO forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        if (!user.getActive()) {
            throw new RuntimeException("User account is deactivated");
        }

        String tempPassword = com.genc.auth_service.util.PasswordGenerator.generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setIsFirstLogin(true); // Force password change on next login
        user = userRepository.save(user);

        boolean emailSent = emailService.sendTemporaryPasswordEmail(user.getEmail(), user.getFullName(), tempPassword, true);
        log.info("Temporary password generated for forgot password request: {}", user.getEmail());

        UserDTO dto = mapToDTO(user);
        dto.setTempPassword(tempPassword);
        dto.setEmailSent(emailSent);
        return dto;
    }

    // Validate token
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    // Map User entity to UserDTO
    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.getActive())
                .isFirstLogin(user.getIsFirstLogin() != null ? user.getIsFirstLogin() : false)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

