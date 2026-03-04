package com.gangku.be.model.review;

import com.gangku.be.constant.user.UserReviewSort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class ReviewPageables {

    private ReviewPageables() {}


    public static Pageable preview(UserReviewSort sort) {
        return PageRequest.of(0, 3, sort.toSpringSort());
    }


    public static Pageable of(int page, int size, UserReviewSort sort) {
        return PageRequest.of(page - 1, size, sort.toSpringSort());
    }
}