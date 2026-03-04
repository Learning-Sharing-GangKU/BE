package com.gangku.be.controller;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.dto.review.ReviewCreateRequestDto;
import com.gangku.be.dto.review.ReviewCreateResponseDto;
import com.gangku.be.model.common.PrefixedId;
import com.gangku.be.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews/{userId}")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewCreateResponseDto> createReview(
            @PathVariable("userId") String revieweeId,
            @Valid @RequestBody ReviewCreateRequestDto reviewCreateRequestDto,
            @AuthenticationPrincipal Long reviewerId
    ) {
        Long internalRevieweeId = PrefixedId.parse(revieweeId).require(ResourceType.USER);

        ReviewCreateResponseDto reviewCreateResponseDto = reviewService.createReview(reviewerId, internalRevieweeId, reviewCreateRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewCreateResponseDto);
    }
}
