package com.genc.auth_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:noreply@hospital.com}")
    private String fromEmail;

    public boolean sendTemporaryPasswordEmail(String toEmail, String fullName, String tempPassword, boolean isReset) {
        String subject = isReset ? "Password Reset - XYZ Hospital" : "Welcome to XYZ Hospital - Account Details";

        String body = String.format(
            "Welcome to XYZ Hospital.\n\n" +
            "Hello %s,\n\n" +
            "%s\n\n" +
            "Username:\n%s\n\n" +
            "Temporary Password:\n%s\n\n" +
            "Please log in using this temporary password.\n" +
            "For security reasons, you must change your password immediately after your first login.\n\n" +
            "Regards,\nXYZ Hospital Administration",
            fullName != null ? fullName : "Patient",
            isReset ? "Your password reset request has been processed. Here is your new temporary password:" : "Your account has been created successfully.",
            toEmail,
            tempPassword
        );

        log.info("=================== TEMPORARY PASSWORD EMAIL ===================");
        log.info("TO: {}", toEmail);
        log.info("SUBJECT: {}", subject);
        log.info("BODY:\n{}", body);
        log.info("=================================================================");

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                log.info("Temporary password email sent successfully to {}", toEmail);
                return true;
            } catch (Exception e) {
                log.error("Failed to send SMTP email to {}: {}. Ensure valid SMTP credentials (e.g. Gmail App Password) are configured in application.properties or environment variables.", toEmail, e.getMessage());
                return false;
            }
        } else {
            log.warn("JavaMailSender is not initialized. Please configure spring.mail properties in application.properties.");
            return false;
        }
    }
}
