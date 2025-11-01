package edu.xtu.bbs.verification.mail;

import edu.xtu.bbs.verification.VerificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class EmailConfig {

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${verification.subject:Your Verification Code}")
    private String defaultSubject;

    @Bean
    @ConditionalOnMissingBean(VerificationSender.class)
    public EmailSender emailSender(JavaMailSender javaMailSender) {
        return new EmailSender(javaMailSender, fromEmail, defaultSubject);
    }
}
