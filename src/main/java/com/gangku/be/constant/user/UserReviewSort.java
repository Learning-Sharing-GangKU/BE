package com.gangku.be.constant.user;

import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
public enum UserReviewSort {
    LATEST("latest");

    private final String sort;

    UserReviewSort(String sort) {
        this.sort = sort;
    }

    public static UserReviewSort from(String value) {
        for (UserReviewSort type : values()) {
            if (type.sort.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new CustomException(CommonErrorCode.INVALID_REQUEST_PARAMETER);
    }

    public Sort toSpringSort() {
        return switch (this) {
            case LATEST -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
        };
    }

    public String toSortedByForSpec() {
        return switch (this) {
            case LATEST -> "createdAt,desc,id,desc";
        };
    }
}
