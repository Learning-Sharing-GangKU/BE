package com.gangku.be.model.review;

import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public class ReviewCursorCodec {

    private ReviewCursorCodec() {}

    public static String encode(ReviewCursor cursor) {
        String raw = cursor.createdAt() + "|" + cursor.id();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static ReviewCursor decode(String encodedCursor) {
        try {
            String raw =
                    new String(
                            Base64.getUrlDecoder().decode(encodedCursor), StandardCharsets.UTF_8);

            String[] parts = raw.split("\\|");
            LocalDateTime createdAt = LocalDateTime.parse(parts[0]);
            Long id = Long.parseLong(parts[1]);

            return new ReviewCursor(createdAt, id);
        } catch (Exception e) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST_PARAMETER);
        }
    }
}
