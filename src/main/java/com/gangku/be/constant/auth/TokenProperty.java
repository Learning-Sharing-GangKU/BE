package com.gangku.be.constant.auth;

import lombok.Getter;

@Getter
public enum TokenProperty {

    ACCESS_TOKEN(1000L * 60 * 30),
    REFRESH_TOKEN(1000L * 60 * 60 * 24 * 14);

    private final long expirationInMillis;

    TokenProperty(long expirationInMillis) {
        this.expirationInMillis = expirationInMillis;
    }

    public long getExpirationInSeconds() {
        return expirationInMillis / 1000;
    }

    public long getExpirationInDays() {
        return this.expirationInMillis / (1000L * 60 * 60 * 24);
    }
}
