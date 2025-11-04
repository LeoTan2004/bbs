package edu.xtu.bbs.user.vo;

import edu.xtu.bbs.user.dto.CreateUserRequest;
import edu.xtu.bbs.verification.VerificationParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterVo {

    private CreateUserRequest user;

    private VerificationParam verification;
}
