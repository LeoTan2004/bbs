package edu.xtu.bbs.verification.mail;

import edu.xtu.bbs.verification.VerificationSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class EmailSender implements VerificationSender {


    private final JavaMailSender javaMailSender;

    private final String fromEmail;

    private final String defaultSubject;

    public EmailSender(JavaMailSender javaMailSender, String fromEmail, String defaultSubject) {
        this.javaMailSender = javaMailSender;
        this.fromEmail = fromEmail;
        this.defaultSubject = defaultSubject;
    }

    protected boolean sendEmail(String to, String subject, String body) {
        final SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        msg.setFrom(fromEmail);
        javaMailSender.send(msg);
        return true;
    }

    public boolean sendCode(String principal, String content) {
        return sendEmail(principal, defaultSubject, content);
    }
}
