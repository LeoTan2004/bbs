package edu.xtu.bbs.verification;

public interface VerificationSender {

    /**
     * Sends a verification code to the specified principal.
     *
     * @param principal the identifier for the user (e.g., email or phone number)
     * @param content   the verification content to send
     * @return true if the code was sent successfully, false otherwise
     */
    boolean sendCode(String principal, String content);
}
