package com.gangku.be.model.auth;

public record EmailVerificationConfirmResult(boolean verified, String email) {}
