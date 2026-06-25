package com.fini.todo.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender javaMailSender;

    public MailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("FINI - Password Reset OTP");
        message.setText("""
                Your OTP code is: %s

                This OTP will expire in 5 minutes.

                If you did not request a password reset, please ignore this email.
                """.formatted(otp));

        javaMailSender.send(message);
    }
}