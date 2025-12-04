package com.gangku.be.model.auth;

public record EmailVerificationSendResult(
        String sessionId,
        String emailVerificationToken,
        Long sessionTtlMinutes
) {}
