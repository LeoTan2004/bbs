package edu.xtu.bbs.verification;

public class VerificationTooFrequentException extends Exception {
    private final Integer verificationId;

    public VerificationTooFrequentException(Integer verificationId) {
        this.verificationId = verificationId;
    }

    @Override
    public String toString() {
        return "VerificationTooFrequentException: " + verificationId;
    }
}
