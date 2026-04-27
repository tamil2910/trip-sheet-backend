package com.example.trip_sheet_backend.services;

import com.example.trip_sheet_backend.models.PasswordResetToken;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.PasswordResetTokenRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private static final int OTP_EXPIRY_MINUTES = 15;

    public PasswordResetService(
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserAccountRepository userAccountRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Generate OTP, store it, and send via email
     */
    public void sendPasswordResetOTP(String email) {
        // Find user by email
        Optional<UserAccount> userOptional = userAccountRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            throw new RuntimeException("No user found with email: " + email);
        }

        UserAccount userAccount = userOptional.get();

        // Generate OTP
        String otpCode = generateOTP();

        // Create and store password reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserAccount(userAccount);
        resetToken.setOtpCode(otpCode);
        resetToken.setExpiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        resetToken.setIsUsed(false);

        passwordResetTokenRepository.save(resetToken);

        // Send OTP via email
        try {
            emailService.sendPasswordResetOTP(userAccount.getEmail(), otpCode);
        } catch (Exception e) {
            // Delete the token if email fails
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    /**
     * Verify OTP and reset password
     */
    public void resetPasswordWithOTP(String email, String otpCode, String newPassword) {
        // Find user by email
        Optional<UserAccount> userOptional = userAccountRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            throw new RuntimeException("No user found with email: " + email);
        }

        UserAccount userAccount = userOptional.get();

        // Find and validate OTP
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository
                .findByOtpCodeAndUserAccount(otpCode, userAccount);

        if (tokenOptional.isEmpty()) {
            throw new RuntimeException("Invalid OTP code");
        }

        PasswordResetToken resetToken = tokenOptional.get();

        if (!resetToken.isValid()) {
            if (resetToken.isExpired()) {
                throw new RuntimeException("OTP has expired. Request a new one");
            } else {
                throw new RuntimeException("OTP has already been used");
            }
        }

        // Update password
        userAccount.setPassword(passwordEncoder.encode(newPassword));
        userAccountRepository.save(userAccount);

        // Mark token as used
        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    /**
     * Generate a 6-digit OTP
     */
    private String generateOTP() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // Generates 6-digit OTP
        return String.valueOf(otp);
    }
}
