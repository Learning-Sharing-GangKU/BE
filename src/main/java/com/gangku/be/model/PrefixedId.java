package com.gangku.be.model;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.ParticipationErrorCode;

public record PrefixedId(
        ResourceType resourceType,
        Long value
) {

    public String toExternal() {
        return resourceType.prefix() + value;
    }

    public static PrefixedId of(ResourceType resourceType, Long value) {
        if (value == null) {
            throw new CustomException(ParticipationErrorCode.INVALID_PARAMETER_FORMAT);
        }
        return new PrefixedId(resourceType, value);
    }

    public static PrefixedId parse(String externalId) {
        if (externalId == null) {
            throw new CustomException(ParticipationErrorCode.INVALID_PARAMETER_FORMAT);
        }

        ResourceType resourceType = ResourceType.fromPrefix(externalId);
        String prefix = resourceType.prefix();

        String numericPart = externalId.substring(prefix.length());
        try {
            Long value = Long.parseLong(numericPart);
            return new PrefixedId(resourceType, value);
        } catch (NumberFormatException e) {
            throw new CustomException(ParticipationErrorCode.INVALID_PARAMETER_FORMAT);
        }
    }

    public Long require(ResourceType expectedType) {
        if (this.resourceType != expectedType) {
            throw new CustomException(ParticipationErrorCode.INVALID_PARAMETER_FORMAT);
        }
        return value;
    }
}
