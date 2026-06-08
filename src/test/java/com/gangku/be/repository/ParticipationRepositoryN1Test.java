package com.gangku.be.repository;

import static org.assertj.core.api.Assertions.*;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.Session;
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
}
