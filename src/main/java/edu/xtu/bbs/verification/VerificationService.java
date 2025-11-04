package edu.xtu.bbs.verification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class VerificationService {

    private final VerificationSender verificationSender;
    private final VerificationRequestRepository verificationRequestRepository;
    @Value("${verification.code.length:6}")
    private int codeLength;

    @Value("${verification.valid.duration:300s}")
    private Duration validDuration;

    @Value("${verification.verify.duration:1s}")
    private Duration verifyDuration;

    @Value("${verification.verify.max:5}")
    private Integer maxValidCount;

    public VerificationService(VerificationSender verificationSender, VerificationRequestRepository verificationRequestRepository) {
        this.verificationSender = verificationSender;
        this.verificationRequestRepository = verificationRequestRepository;
    }

    protected String generateCode() {

        SecureRandom secureRandom = new SecureRandom();
        return String.format("%0" + codeLength + "d",
                secureRandom.nextInt((int) Math.pow(10, codeLength)));
    }

    protected String generateToken() {
        return UUID.randomUUID().toString();
    }

    public String sendCode(String principal, String action) {
        final String code = generateCode();
        final String msg = VerificationMsgTemplate.of(principal, code, action);
        // Send the code using the VerificationSender
        boolean sent = verificationSender.sendCode(principal, msg);

        // If the code was sent successfully, save it in the cache
        if (sent) {
            final String token = generateToken();
            final VerificationRequest verificationRequest = new VerificationRequest();
            verificationRequest.setPrinciple(principal);
            verificationRequest.setScope(action);
            verificationRequest.setCredential(code);
            verificationRequest.setToken(token);
            verificationRequest.setRequestedAt(Instant.now());
            verificationRequest.setExpiresAt(Instant.now().plus(validDuration));
            verificationRequest.setValidCount(0);
            verificationRequest.setStatus(VerificationStatus.Pending);
            final VerificationRequest save = verificationRequestRepository.save(verificationRequest);
            return save.getToken();
        }
        return "";
    }

    public boolean verifyCode(VerificationParam param)
            throws VerificationRequestNotFoundException,
            VerificationExpiredException,
            VerificationTooFrequentException {

        final Optional<VerificationRequest> verificationRequest =
                verificationRequestRepository.findByPrincipleAndToken(param.principle(), param.token());
        final VerificationRequest request = verificationRequest.orElseThrow(VerificationRequestNotFoundException::new);
        final Instant now = Instant.now();

        // Check the verification was valid
        if (request.getExpiresAt().isBefore(now) || request.getStatus() != VerificationStatus.Pending) {
            throw new VerificationExpiredException();
        }

        // Check the request was not too frequent
        Instant lastValidAt = request.getLastValidAt();
        int currentValidCount = request.getValidCount() != null ? request.getValidCount() : 0;

        if ((lastValidAt != null && lastValidAt.plus(verifyDuration).isAfter(now)) ||
                currentValidCount >= maxValidCount) {
            throw new VerificationTooFrequentException(request.getId());
        }

        // Record some basic audit info
        request.setValidCount(currentValidCount + 1);
        request.setLastValidAt(now);
        if (Objects.equals(request.getCredential(), param.credential())
                && Objects.equals(request.getScope(), param.scope())) {
            request.setStatus(VerificationStatus.Verified);
            verificationRequestRepository.save(request);
            return true;
        }
        verificationRequestRepository.save(request);
        return false;
    }


}
