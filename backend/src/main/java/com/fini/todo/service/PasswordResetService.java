package com.fini.todo.service;

import com.fini.todo.dto.request.ForgotPasswordRequest;
import com.fini.todo.dto.request.ResetPasswordRequest;
import com.fini.todo.dto.response.MessageResponse;
import com.fini.todo.entity.PasswordResetToken;
import com.fini.todo.entity.User;
import com.fini.todo.repository.PasswordResetTokenRepository;
import com.fini.todo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.reset.otp-expiration-minutes}")
    private long otpExpirationMinutes;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null) {
            return new MessageResponse("If this email exists, an OTP has been sent");
        }

        markOldOtpsAsUsed(user);

        String otp = generateOtp();

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setOtpHash(passwordEncoder.encode(otp));
        token.setExpiredAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        token.setUsed(false);

        passwordResetTokenRepository.save(token);

        try {
            mailService.sendOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {
            System.out.println("======================================");
            System.out.println("[DEV MODE] Failed to send email.");
            System.out.println("[DEV MODE] OTP for " + user.getEmail() + " is: " + otp);
            System.out.println("======================================");
        }

        return new MessageResponse("If this email exists, an OTP has been sent");
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        PasswordResetToken token = getValidLatestToken(user, request.getOtp());

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new RuntimeException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        return new MessageResponse("Password has been reset successfully");
    }

    private PasswordResetToken getValidLatestToken(User user, String rawOtp) {
        PasswordResetToken token = passwordResetTokenRepository
                .findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (token.getExpiredAt().isBefore(LocalDateTime.now())) {
            token.setUsed(true);
            passwordResetTokenRepository.save(token);
            throw new RuntimeException("OTP has expired");
        }

        boolean otpMatches = passwordEncoder.matches(rawOtp, token.getOtpHash());

        if (!otpMatches) {
            int attempts = token.getFailedAttempts() == null ? 0 : token.getFailedAttempts();
            attempts++;
            token.setFailedAttempts(attempts);
            if (attempts >= 5) {
                token.setUsed(true);
                passwordResetTokenRepository.save(token);
                throw new RuntimeException("OTP has been invalidated due to too many failed attempts");
            }
            passwordResetTokenRepository.save(token);
            throw new RuntimeException("Invalid OTP");
        }

        return token;
    }

    private void markOldOtpsAsUsed(User user) {
        List<PasswordResetToken> oldTokens =
                passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId());

        for (PasswordResetToken oldToken : oldTokens) {
            oldToken.setUsed(true);
        }

        passwordResetTokenRepository.saveAll(oldTokens);
    }

    private String generateOtp() {
        int number = secureRandom.nextInt(1_000_000);
        return String.format("%06d", number);
    }
}
