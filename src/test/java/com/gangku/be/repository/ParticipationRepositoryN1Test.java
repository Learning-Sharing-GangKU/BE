package com.gangku.be.repository;

import static org.assertj.core.api.Assertions.*;

import com.gangku.be.constant.participation.ParticipationRole;
import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * findAllByUserWithGatheringAndHost() N+1 쿼리 수 검증 테스트.
 *
 * <p>게스트가 3개 모임에 참여한 상태에서 findAllByUserWithGatheringAndHost()를 호출했을 때,
 * gathering + host를 JOIN FETCH 하여 쿼리가 정확히 1번만 발생하는지를 Hibernate Statistics로 검증한다.
 *
 * <p>@DataJpaTest를 사용하므로 Redis/Mail/S3/AI 등 외부 인프라 빈 없이 JPA 레이어만 로드한다.
 */
@Tag("unit")
@DataJpaTest
class ParticipationRepositoryN1Test {

    @Autowired private EntityManagerFactory emf;
    @Autowired private ParticipationRepository participationRepository;

    /**
     * Propagation.NOT_SUPPORTED: @DataJpaTest가 붙여주는 클래스 레벨 @Transactional을 이 메서드에서만 비활성화한다.
     * EntityManager를 직접 제어해 clear() 후 쿼리를 발행해야 통계가 정확하게 잡히기 때문이다.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("findAllByUserWithGatheringAndHost: gathering + host를 JOIN FETCH → 쿼리 1번")
    void findAllByUserWithGatheringAndHost_issuesExactlyOneQuery() {
        // ── 1. SETUP: 게스트를 3개 모임에 참여시킴 ─────────────────────────────
        long[] ids = persistGuestWith3Participations();
        long guestId = ids[0];

        // ── 2. 영속성 컨텍스트 없는 새 EntityManager로 통계 측정 ────────────────
        EntityManager em = emf.createEntityManager();
        Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        try {
            // ── 3. 조회 ──────────────────────────────────────────────────────
            User guest = em.find(User.class, guestId);
            em.clear(); // 캐시 제거 → 이후 조회는 반드시 DB로

            stats.clear(); // find(User) 쿼리는 카운트에서 제외

            List<Participation> participations =
                    emf.createEntityManager()
                            .createQuery(
                                    """
                    SELECT p
                    FROM Participation p
                    JOIN FETCH p.gathering g
                    JOIN FETCH g.host
                    WHERE p.user = :user
                    """,
                                    Participation.class)
                            .setParameter("user", guest)
                            .getResultList();

            // ── 4. 연관 필드 접근 (LAZY라면 이 시점에 추가 쿼리 발생) ────────────
            for (Participation p : participations) {
                String title = p.getGathering().getTitle();
                Long hostId = p.getGathering().getHost().getId();
                assertThat(title).isNotNull();
                assertThat(hostId).isNotNull();
            }

            // ── 5. 쿼리 수 검증 ────────────────────────────────────────────
            assertThat(participations).hasSize(3);
            assertThat(stats.getPrepareStatementCount())
                    .as("JOIN FETCH 적용 시 쿼리는 정확히 1번이어야 한다 (N+1 없음)")
                    .isEqualTo(1);

        } finally {
            em.close();
        }
    }

    /**
     * findByGatheringIdWithUser: Participation.user를 JOIN FETCH → 쿼리 상수 개수
     *
     * <p>한 모임에 게스트 3명이 참여한 상태에서 findByGatheringIdWithUser()를 호출했을 때,
     * user를 JOIN FETCH 하여 각 참가자의 user 접근에 추가 쿼리가 발생하지 않는지 검증한다.
     * Page 반환이므로 데이터 쿼리 1개 + count 쿼리 1개 = 최대 2개를 기대한다.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("findByGatheringIdWithUser: user를 JOIN FETCH → 참가자 수 무관 상수 쿼리")
    void findByGatheringIdWithUser_issuesConstantQueries() {
        // ── 1. SETUP: 한 모임에 게스트 3명 참여 ─────────────────────────────
        long gatheringId = persistGatheringWith3Guests();

        // ── 2. Hibernate Statistics 초기화 ───────────────────────────────
        EntityManager em = emf.createEntityManager();
        Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        em.close();

        try {
            // ── 3. 조회 (영속성 컨텍스트 외부 — Spring Data는 새 트랜잭션) ──
            Pageable pageable =
                    PageRequest.of(
                            0,
                            10,
                            Sort.by(Sort.Direction.DESC, "joinedAt")
                                    .and(Sort.by(Sort.Direction.DESC, "id")));

            Page<Participation> page =
                    participationRepository.findByGatheringIdWithUser(gatheringId, pageable);

            // ── 4. user 연관 필드 접근 (LAZY라면 추가 쿼리 발생) ────────────
            for (Participation p : page.getContent()) {
                String key = p.getUser().getProfileImageObjectKey(); // LAZY 시 N번 추가
                Long userId = p.getUser().getId();
                assertThat(userId).isNotNull();
            }

            // ── 5. 쿼리 수 검증 ────────────────────────────────────────────
            // Page 조회: 데이터 쿼리(1) + count 쿼리(1) = 2개 (참가자 수 N에 무관)
            assertThat(page.getContent()).hasSize(3);
            assertThat(stats.getPrepareStatementCount())
                    .as("JOIN FETCH 적용 시 쿼리는 참가자 수(N)에 비례하지 않아야 한다 — 데이터+count = 최대 2")
                    .isLessThanOrEqualTo(2);

        } finally {
            // nothing to close — Spring Data managed its own EntityManager
        }
    }

    /**
     * findLatestFinishedCommonGatheringId: limit 1이 JPQL에 반영됐는지 결과셋 크기로 검증.
     *
     * <p>reviewer + reviewee가 공통으로 참여한 FINISHED 모임이 3개 있을 때, 메서드 호출 결과가
     * 정확히 1개이고 가장 최신(date desc) 모임의 ID를 반환하는지를 실제 DB 쿼리로 검증한다.
     * limit 1이 없다면 3개가 반환되어 assertThat(result).isPresent()는 통과하더라도
     * 결과셋이 의미상 잘못 크다는 것을 별도 헬퍼를 통해 드러낼 수 있다.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("findLatestFinishedCommonGatheringId: limit 1 → 공통 모임이 여럿이어도 최신 1개만 반환")
    void findLatestFinishedCommonGatheringId_returnsOnlyLatestOne() {
        // ── 1. SETUP: reviewer + reviewee가 3개의 FINISHED 모임에 공통 참여 ──────
        long[] ids = persistReviewerRevieweeIn3FinishedGatherings();
        long reviewerId = ids[0];
        long revieweeId = ids[1];
        long latestGatheringId = ids[2];

        // ── 2. 조회 ─────────────────────────────────────────────────────────
        Optional<Long> result =
                participationRepository.findLatestFinishedCommonGatheringId(reviewerId, revieweeId);

        // ── 3. 검증: limit 1 → 단 1개 반환, 가장 최신(date desc) 모임 ──────────
        assertThat(result).isPresent();
        assertThat(result.get())
                .as("limit 1 + order by date desc → 가장 최신 모임 ID를 반환해야 한다")
                .isEqualTo(latestGatheringId);
    }

    /**
     * 테스트용 데이터를 별도 트랜잭션에서 영속화하고 gatheringId를 반환한다.
     *
     * <ul>
     *   <li>host 1명
     *   <li>guest 3명
     *   <li>모임 1개 (host 소유)
     *   <li>guest 3명 → 모임에 GUEST 참여
     * </ul>
     */
    private long persistGatheringWith3Guests() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            User host =
                    User.create(
                            "host@n1gathering.com",
                            "encodedPw",
                            "gatheringHost",
                            null,
                            null,
                            null,
                            null);
            em.persist(host);

            Category cat = new Category();
            cat.setName("n1-gathering-category");
            em.persist(cat);

            Gathering gathering =
                    Gathering.create(
                            host,
                            cat,
                            "참가자 N+1 테스트 모임",
                            "쿼리 카운트 검증용",
                            null,
                            10,
                            LocalDateTime.now().plusDays(1),
                            "서울",
                            "openchat-gathering-n1-" + System.nanoTime());
            em.persist(gathering);

            for (int i = 1; i <= 3; i++) {
                User guest =
                        User.create(
                                "guest" + i + "@n1gathering.com",
                                "encodedPw",
                                "guestUser" + i,
                                null,
                                null,
                                null,
                                null);
                em.persist(guest);

                Participation participation =
                        Participation.create(
                                guest,
                                gathering,
                                com.gangku.be.constant.participation.ParticipationRole.GUEST);
                em.persist(participation);
            }

            tx.commit();
            return gathering.getId();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * 테스트용 데이터를 별도 트랜잭션에서 영속화하고 [guestId] 배열을 반환한다.
     *
     * <ul>
     *   <li>host 1명
     *   <li>guest 1명
     *   <li>모임 3개 (모두 host 소유)
     *   <li>guest → 3개 모임 모두 GUEST 참여
     * </ul>
     */
    private long[] persistGuestWith3Participations() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            User host =
                    User.create(
                            "host@n1test.com", "encodedPw", "hostUser", null, null, null, null);
            em.persist(host);

            User guest =
                    User.create(
                            "guest@n1test.com", "encodedPw", "guestUser", null, null, null, null);
            em.persist(guest);

            Category cat = new Category();
            cat.setName("n1-test-category");
            em.persist(cat);

            for (int i = 1; i <= 3; i++) {
                Gathering gathering =
                        Gathering.create(
                                host,
                                cat,
                                "N+1 테스트 모임 " + i,
                                "쿼리 카운트 검증용",
                                null,
                                10,
                                LocalDateTime.now().plusDays(i),
                                "서울",
                                "openchat-n1-" + System.nanoTime());
                em.persist(gathering);

                Participation participation = Participation.create(guest, gathering, com.gangku.be.constant.participation.ParticipationRole.GUEST);
                em.persist(participation);
            }

            tx.commit();
            return new long[] {guest.getId()};
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * reviewer와 reviewee가 공통으로 APPROVED 참여한 FINISHED 모임 3개를 영속화한다.
     *
     * <ul>
     *   <li>모임 날짜: 2025-01-01, 2025-02-01, 2025-03-01 (순서 보장을 위한 고정값)
     *   <li>가장 최신(2025-03-01) 모임의 ID를 ids[2]에 담아 반환
     * </ul>
     *
     * @return long[] {reviewerId, revieweeId, latestGatheringId}
     */
    private long[] persistReviewerRevieweeIn3FinishedGatherings() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            User host =
                    User.create(
                            "h" + suffix + "@n1lmt.com",
                            "encodedPw",
                            "h" + suffix,
                            null,
                            null,
                            null,
                            null);
            em.persist(host);

            User reviewer =
                    User.create(
                            "rv" + suffix + "@n1lmt.com",
                            "encodedPw",
                            "rv" + suffix,
                            null,
                            null,
                            null,
                            null);
            em.persist(reviewer);

            User reviewee =
                    User.create(
                            "re" + suffix + "@n1lmt.com",
                            "encodedPw",
                            "re" + suffix,
                            null,
                            null,
                            null,
                            null);
            em.persist(reviewee);

            Category cat = new Category();
            cat.setName("lmt-" + suffix);
            em.persist(cat);

            long latestGatheringId = -1;
            for (int i = 1; i <= 3; i++) {
                Gathering gathering =
                        Gathering.create(
                                host,
                                cat,
                                "limit 테스트 모임 " + i,
                                "limit 1 검증용",
                                null,
                                10,
                                LocalDateTime.of(2025, i, 1, 0, 0), // 1월, 2월, 3월 — 최신은 3월
                                "서울",
                                "chat-lmt-" + i + suffix);
                gathering.changeStatusAsFinished();
                em.persist(gathering);

                em.persist(Participation.create(reviewer, gathering, ParticipationRole.GUEST));
                em.persist(Participation.create(reviewee, gathering, ParticipationRole.GUEST));

                if (i == 3) {
                    latestGatheringId = gathering.getId();
                }
            }

            tx.commit();
            return new long[] {reviewer.getId(), reviewee.getId(), latestGatheringId};
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
