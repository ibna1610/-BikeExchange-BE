package com.bikeexchange.service.service.impl;

import com.bikeexchange.model.User;
import com.bikeexchange.service.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String smtpUsername;

    @Value("${spring.mail.from-email}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.forgot-password.url:http://localhost:3000/reset-password}")
    private String forgotPasswordUrl;

    @Value("${app.name:BikeExchange}")
    private String appName;

    @Async
    @Override
    public void sendVerificationEmail(User user, String token) {
        try {
            log.info("Starting to send verification email to: {}", user.getEmail());
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            Context context = new Context();
            context.setVariable("name", user.getFullName());
            context.setVariable("verificationUrl", frontendUrl + "/verify?token=" + token);

            String html = templateEngine.process("verification-email", context);

            helper.setFrom(fromEmail, appName + " Support");
            helper.setTo(user.getEmail());
            helper.setSubject("Xác thực tài khoản " + appName);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    @Override
    public void sendResetPasswordEmail(User user, String token) {
        try {
            log.info("Starting to send password reset email to: {}", user.getEmail());
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            Context context = new Context();
            context.setVariable("name", user.getFullName());
            context.setVariable("resetUrl", forgotPasswordUrl + "?token=" + token);

            String html = templateEngine.process("reset-password-email", context);

            helper.setFrom(fromEmail, appName + " Support");
            helper.setTo(user.getEmail());
            helper.setSubject("Đặt lại mật khẩu " + appName);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
