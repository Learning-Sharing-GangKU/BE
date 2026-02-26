package com.gangku.be.constant.gathering;

import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import lombok.Getter;

@Getter
public enum GatheringSort {
    LATEST("latest"),
    POPULAR("popular");
    //    RECOMMENDED("recommended")

    private final String sort;

    GatheringSort(String sort) {
        this.sort = sort;
    }

    public static GatheringSort from(String value) {
        for (GatheringSort type : values()) {
            if (type.sort.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new CustomException(CommonErrorCode.INVALID_REQUEST_PARAMETER);
    }
}
