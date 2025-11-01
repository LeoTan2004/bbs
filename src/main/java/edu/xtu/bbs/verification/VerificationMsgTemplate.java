package edu.xtu.bbs.verification;

public interface VerificationMsgTemplate {
    static String of(String principal, String code, String action) {
        return String.format("""
                Hello 【%s】,
                You are performing the action: 【%s】.
                Your verification code is: 【%s】.
                Please use this code to complete your verification process.
                Thank you for using our service!
                """, principal, action, code);
    }
}
