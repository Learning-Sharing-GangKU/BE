package com.gangku.be.constant.user;

import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
public enum UserReviewSort {
    CREATED_AT_DESC("createdAt,desc", Sort.Direction.DESC),
    CREATED_AT_ASC("createdAt,asc", Sort.Direction.ASC);

    private final String sort;
    private final Sort.Direction direction;

    UserReviewSort(String sort, Sort.Direction direction) {
        this.sort = sort;
        this.direction = direction;
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
        return Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
    }

    public String toSortedByForSpec() {
        return this.sort;
    }
}
