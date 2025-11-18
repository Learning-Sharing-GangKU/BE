package com.gangku.be.model;

public record EmailVerificationSendResult(
        String sessionId,
        String emailVerificationToken,
        Long sessionTtlMinutes
) {}
