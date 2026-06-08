package com.gangku.be.repository;

import static org.assertj.core.api.Assertions.*;

import com.gangku.be.constant.participation.ParticipationRole;
import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.Review;
import com.gangku.be.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReviewRepository N+1 쿼리 수 검증 테스트.
 *
 * <p>리뷰 3건이 있는 상태에서 각 조회 메서드를 호출했을 때, reviewer를 JOIN FETCH 하여 reviewer 접근 시 추가 쿼리가 발생하지 않는지
 * Hibernate Statistics로 검증한다.
 *
 * <p>@DataJpaTest를 사용하므로 Redis/Mail/S3/AI 등 외부 인프라 빈 없이 JPA 레이어만 로드한다.
 */
@Tag("unit")
@DataJpaTest
class ReviewRepositoryN1Test {

    @Autowired private EntityManagerFactory emf;
    @Autowired private ReviewRepository reviewRepository;

    // ── findByRevieweeId (Page 반환) ──────────────────────────────────────────

    /**
     * Propagation.NOT_SUPPORTED: @DataJpaTest가 붙여주는 클래스 레벨 @Transactional을 이 메서드에서만 비활성화한다. 영속성
     * 컨텍스트 캐시 없이 실제 DB 쿼리 수를 측정하기 위함이다.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("findByRevieweeId: reviewer를 JOIN FETCH → Page 조회 시 쿼리 ≤ 2개 (데이터+count)")
    void findByRevieweeId_fetchesReviewerInConstantQueries() {
        // ── 1. SETUP: reviewee 1명 + reviewer 3명 + 리뷰 3건 ──────────────
        long revieweeId = persistRevieweeWith3Reviews();

        // ── 2. Hibernate Statistics 초기화 ───────────────────────────────
        EntityManager em = emf.createEntityManager();
        Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        em.close();

        try {
            // ── 3. 조회 ───────────────────────────────────────────────────
            Pageable pageable =
                    PageRequest.of(
                            0,
                            10,
                            Sort.by(Sort.Direction.DESC, "createdAt")
                                    .and(Sort.by(Sort.Direction.DESC, "id")));

            Page<Review> page = reviewRepository.findByRevieweeId(revieweeId, pageable);

            // ── 4. reviewer 필드 접근 (LAZY였다면 N번 추가 쿼리) ──────────
            for (Review r : page.getContent()) {
                String key = r.getReviewer().getProfileImageObjectKey(); // N+1 발생 지점
                Long reviewerId = r.getReviewer().getId();
                assertThat(reviewerId).isNotNull();
            }

            // ── 5. 쿼리 수 검증 ──────────────────────────────────────────
            // Page 반환: 데이터 쿼리(1) + count 쿼리(1) = 최대 2개 (리뷰 수 N에 무관)
            assertThat(page.getContent()).hasSize(3);
            assertThat(stats.getPrepareStatementCount())
                    .as("JOIN FETCH 적용 시 쿼리는 reviewer 수(N)에 비례하지 않아야 한다 — 데이터+count = 최대 2")
                    .isLessThanOrEqualTo(2);

        } finally {
            // Spring Data가 자체 EntityManager를 관리하므로 별도 close 불필요
        }
    }

    // ── findFirstPageByRevieweeId (List 반환) ─────────────────────────────────

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("findFirstPageByRevieweeId: reviewer를 JOIN FETCH → List 조회 시 쿼리 정확히 1개")
    void findFirstPageByRevieweeId_fetchesReviewerInOneQuery() {
        // ── 1. SETUP ──────────────────────────────────────────────────────
        long revieweeId = persistRevieweeWith3Reviews();

        // ── 2. Statistics 초기화 ──────────────────────────────────────────
        EntityManager em = emf.createEntityManager();
        Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        em.close();

        try {
            // ── 3. 조회 ───────────────────────────────────────────────────
            Pageable pageable =
                    PageRequest.of(
                            0,
                            10,
                            Sort.by(Sort.Direction.DESC, "createdAt")
                                    .and(Sort.by(Sort.Direction.DESC, "id")));

            List<Review> reviews = reviewRepository.findFirstPageByRevieweeId(revieweeId, pageable);

            // ── 4. reviewer 필드 접근 ────────────────────────────────────
            for (Review r : reviews) {
                String key = r.getReviewer().getProfileImageObjectKey();
                Long reviewerId = r.getReviewer().getId();
                assertThat(reviewerId).isNotNull();
            }

            // ── 5. 쿼리 수 검증 ──────────────────────────────────────────
            // List 반환: count 쿼리 없음 → 정확히 1개
            assertThat(reviews).hasSize(3);
            assertThat(stats.getPrepareStatementCount())
                    .as("JOIN FETCH 적용 시 List 조회는 쿼리 정확히 1개이어야 한다 (N+1 없음)")
                    .isEqualTo(1);

        } finally {
            // no-op
        }
    }

    // ── findNextPageByRevieweeIdAndCursorDesc (커서 기반 List 반환) ───────────

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("findNextPageByRevieweeIdAndCursorDesc: reviewer를 JOIN FETCH → 커서 조회 시 쿼리 정확히 1개")
    void findNextPageByRevieweeIdAndCursorDesc_fetchesReviewerInOneQuery() {
        // ── 1. SETUP ──────────────────────────────────────────────────────
        long revieweeId = persistRevieweeWith3Reviews();

        // ── 2. Statistics 초기화 ──────────────────────────────────────────
        EntityManager em = emf.createEntityManager();
        Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        em.close();

        try {
            // ── 3. 커서: 미래 시점을 기준으로 모든 리뷰가 cursor 이전에 있도록 설정 ──
            LocalDateTime futureCursor = LocalDateTime.now().plusYears(1);
            long cursorId = Long.MAX_VALUE;

            Pageable pageable = PageRequest.of(0, 10);

            List<Review> reviews =
                    reviewRepository.findNextPageByRevieweeIdAndCursorDesc(
                            revieweeId, futureCursor, cursorId, pageable);

            // ── 4. reviewer 필드 접근 ────────────────────────────────────
            for (Review r : reviews) {
                String key = r.getReviewer().getProfileImageObjectKey();
                Long reviewerId = r.getReviewer().getId();
                assertThat(reviewerId).isNotNull();
            }

            // ── 5. 쿼리 수 검증 ──────────────────────────────────────────
            // List 반환: count 쿼리 없음 → 정확히 1개
            assertThat(reviews).hasSize(3);
            assertThat(stats.getPrepareStatementCount())
                    .as("JOIN FETCH 적용 시 커서 조회는 쿼리 정확히 1개이어야 한다 (N+1 없음)")
                    .isEqualTo(1);

        } finally {
            // no-op
        }
    }

    // ── 테스트 데이터 헬퍼 ─────────────────────────────────────────────────────

    /**
     * 테스트용 데이터를 별도 트랜잭션에서 영속화하고 revieweeId를 반환한다.
     *
     * <ul>
     *   <li>host 1명 (gathering 소유자)
     *   <li>reviewee 1명 (리뷰 받는 사람)
     *   <li>reviewer 3명 (각각 리뷰 작성)
     *   <li>gathering 1개
     *   <li>review 3건 (reviewer 3명 → reviewee)
     * </ul>
     */
    private long persistRevieweeWith3Reviews() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            // NOT_SUPPORTED 테스트는 롤백이 없으므로 이메일/닉네임을 유니크하게 만든다
            // UUID 앞 8자리 사용 (닉네임 20자 제한 준수, 충돌 없음)
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            // 닉네임 최대 20자 제한: suffix 8자 + 접두어 최대 4자 = 12자 이하 유지
            User host =
                    User.create(
                            "h" + suffix + "@n1r.com",
                            "encodedPw",
                            "h" + suffix,
                            null,
                            null,
                            null,
                            null);
            em.persist(host);

            User reviewee =
                    User.create(
                            "ee" + suffix + "@n1r.com",
                            "encodedPw",
                            "ee" + suffix,
                            null,
                            null,
                            null,
                            null);
            em.persist(reviewee);

            Category cat = new Category();
            cat.setName("cat-" + suffix);
            em.persist(cat);

            Gathering gathering =
                    Gathering.create(
                            host,
                            cat,
                            "리뷰 N+1 테스트 모임",
                            "쿼리 카운트 검증용",
                            null,
                            10,
                            LocalDateTime.now().plusDays(1),
                            "서울",
                            "chat-" + suffix);
            em.persist(gathering);

            Participation revieweeParticipation =
                    Participation.create(reviewee, gathering, ParticipationRole.GUEST);
            em.persist(revieweeParticipation);

            for (int i = 1; i <= 3; i++) {
                User reviewer =
                        User.create(
                                "r" + i + suffix + "@n1r.com",
                                "encodedPw",
                                "r" + i + suffix,
                                null,
                                null,
                                null,
                                null);
                em.persist(reviewer);

                Review review = Review.create(reviewer, reviewee, gathering, 5 - i, "리뷰 내용 " + i);
                em.persist(review);
            }

            tx.commit();
            return reviewee.getId();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
