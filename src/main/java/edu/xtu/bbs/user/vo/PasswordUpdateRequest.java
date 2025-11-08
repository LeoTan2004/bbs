package edu.xtu.bbs.user.vo;

import edu.xtu.bbs.verification.VerificationParam;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordUpdateRequest {
    @Email
    private String email;
    @Size(min = 8, max = 20)
    private String password;
    private VerificationParam verification;
}
