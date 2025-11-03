package edu.xtu.bbs.verification.mail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TEST_MAIL", matches = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
class EmailSenderTest {

    @Value("${TEST_MAIL:test@mail}")
    private String testMail;
    @Autowired
    private EmailSender emailSender;

    @Test
    void sendEmail() {
        final boolean b = emailSender.sendCode(testMail, "123456");
        Assertions.assertTrue(b);
    }
}