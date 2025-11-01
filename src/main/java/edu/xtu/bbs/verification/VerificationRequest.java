package edu.xtu.bbs.verification;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "verification_request")
public class VerificationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 255)
    @NotNull
    @Column(name = "principle", nullable = false)
    private String principle;

    @Size(max = 255)
    @NotNull
    @Column(name = "scope", nullable = false)
    private String scope;

    @Size(max = 255)
    @NotNull
    @Column(name = "token", nullable = false)
    private String token;

    @Size(max = 255)
    @NotNull
    @Column(name = "credential", nullable = false)
    private String credential;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private VerificationStatus status = VerificationStatus.Pending;

    @Column(name = "valid_count")
    private Integer validCount;

    @Column(name = "last_valid_at")
    private Instant lastValidAt;

}