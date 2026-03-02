package com.gangku.be.constant.auth;

public class EmailConstants {

    private EmailConstants() {
    }

    public static final String VERIFICATION_PATH = "/api/v1/auth/email/verification/start?token=";

    public static final String VERIFICATION_SUBJECT = "[GangKU] 이메일 인증을 완료해주세요.";

    public static final String VERIFICATION_BODY_TEMPLATE =
            """
                    안녕하세요, GangKU 입니다.
                    아래 링크를 클릭하면 이메일 인증이 완료됩니다.
                    클릭한 후에 다시 회원가입 페이지로 돌아가주십시오.
                    
                    %s
                    
                    (유효시간: %d분)
                    """;
}
