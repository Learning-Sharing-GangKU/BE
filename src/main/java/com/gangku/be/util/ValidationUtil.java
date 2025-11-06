package com.gangku.be.util;


import java.net.URL;
import java.util.regex.Pattern;



public class ValidationUtil {
    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PASSWORD_REGEX =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_REGEX.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_REGEX.matcher(password).matches();
    }

    public static boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();  // URL 객체를 만들고 URI로 변환까지 성공해야 유효한 URL로 간주
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
