package com.gangku.be.constant.auth;

public final class RedisKeys {

    private RedisKeys() {}

    public static final String SIGNUP_SESSION_PREFIX = "auth:signup:session:";
    public static final String EMAIL_VERIFICATION_TOKEN_PREFIX =
            "auth:signup:email-verification-token:";
    public static final String VERIFIED_EMAIL_PREFIX = "auth:signup:verified-email:";

    public static String signupSessionKey(String sessionId) {
        return SIGNUP_SESSION_PREFIX + sessionId;
    }

    public static String emailVerificationTokenKey(String tokenId) {
        return EMAIL_VERIFICATION_TOKEN_PREFIX + tokenId;
    }

    public static String verifiedEmailKey(String email) {
        return VERIFIED_EMAIL_PREFIX + email;
    }
}
