package com.gangku.be.constant.auth;

import lombok.Getter;

@Getter
public enum CookieProperty {

    REFRESH_TOKEN_COOKIE_NAME("refresh_token"),
    SIGNUP_SESSION_COOKIE_NAME("signup_session");

    private final String cookieName;

    CookieProperty(String cookieName) {
        this.cookieName = cookieName;
    }
}
