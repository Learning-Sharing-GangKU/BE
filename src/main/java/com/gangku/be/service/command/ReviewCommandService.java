package com.gangku.be.service.command;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Review;
import com.gangku.be.domain.User;
import com.gangku.be.dto.review.ReviewCreateRequestDto;
import com.gangku.be.dto.review.ReviewCreateResponseDto;
import com.gangku.be.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public ReviewCreateResponseDto saveReview(
            User reviewer,
            User reviewee,
            Gathering gathering,
            ReviewCreateRequestDto reviewCreateRequestDto) {

        Review review =
                Review.create(
                        reviewer,
                        reviewee,
                        gathering,
                        reviewCreateRequestDto.getRating(),
                        reviewCreateRequestDto.getComment());

        reviewRepository.save(review);

        return ReviewCreateResponseDto.from(review);
    }
}