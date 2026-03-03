package com.gangku.be.repository;

import com.gangku.be.domain.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
@DataJpaTest
class ReviewRepositoryTest {

    @Autowired ReviewRepository reviewRepository;
    @Autowired EntityManager em;

    @Test
    void findByRevieweeId_returnsPagedReviews() {
        // given
        User reviewer1 = persistUser("a@test.com", "a");
        User reviewer2 = persistUser("b@test.com", "b");
        User reviewee = persistUser("c@test.com", "c");
        Gathering g = persistGathering(reviewee);

        reviewRepository.save(Review.create(reviewer1, reviewee, g, 5, "good"));
        reviewRepository.save(Review.create(reviewer2, reviewee, g, 4, "nice"));
        em.flush();
        em.clear();

        // when
        Page<Review> page = reviewRepository.findByRevieweeId(reviewee.getId(), PageRequest.of(0, 1));

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByIdAndReviewerId_allowsOnlyWriter() {
        // given
        User reviewer = persistUser("a@test.com", "a");
        User other = persistUser("b@test.com", "b");
        User reviewee = persistUser("c@test.com", "c");
        Gathering g = persistGathering(reviewee);

        Review saved = reviewRepository.save(Review.create(reviewer, reviewee, g, 5, "good"));
        em.flush();
        em.clear();

        // when & then
        assertThat(reviewRepository.findByIdAndReviewerId(saved.getId(), reviewer.getId())).isPresent();
        assertThat(reviewRepository.findByIdAndReviewerId(saved.getId(), other.getId())).isEmpty();
    }

    @Test
    void uniqueConstraint_sameGatheringSamePair_onlyOneReviewAllowed() {
        // given
        User reviewer = persistUser("a@test.com", "a");
        User reviewee = persistUser("b@test.com", "b");
        Gathering g = persistGathering(reviewee);

        reviewRepository.save(Review.create(reviewer, reviewee, g, 5, "first"));
        em.flush();

        // when
        Review dup = Review.create(reviewer, reviewee, g, 4, "dup");

        // then
        assertThatThrownBy(() -> {
            reviewRepository.save(dup);
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByGatheringIdAndRevieweeIdAndReviewerId_works() {
        // given
        User reviewer = persistUser("a@test.com", "a");
        User reviewee = persistUser("b@test.com", "b");
        Gathering g = persistGathering(reviewee);

        reviewRepository.save(Review.create(reviewer, reviewee, g, 5, "ok"));
        em.flush();
        em.clear();

        // when
        boolean exists = reviewRepository.existsByGatheringIdAndReviewerIdAndRevieweeId(
                g.getId(), reviewer.getId(), reviewee.getId()
        );

        // then
        assertThat(exists).isTrue();
    }

    private User persistUser(String email, String nickname) {
        User u = User.create(email, "encodedPw", nickname, null, null, null, null);
        em.persist(u);
        return u;
    }

    private Gathering persistGathering(User host) {
        Category c = new Category();
        c.setName("test");
        em.persist(c);

        Gathering g = Gathering.create(
                host, c, "title", "desc", null,
                10, LocalDateTime.now().plusDays(1),
                "loc", "openchat-" + host.getId() + "-" + System.nanoTime()
        );
        em.persist(g);
        return g;
    }
}