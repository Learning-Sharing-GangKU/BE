package com.gangku.be.constant.id;

import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.ParticipationErrorCode;

public enum ResourceType {
    USER("usr_"),
    GATHERING("gath_"),
    PARTICIPATION("part_");

    private final String prefix;

    ResourceType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    public static ResourceType fromPrefix(String rawId) {
        for (ResourceType resourceType : values()) {
            if (rawId.startsWith(resourceType.prefix)) {
                return resourceType;
            }
        }
        throw new CustomException(ParticipationErrorCode.INVALID_PARAMETER_FORMAT);
    }
}
