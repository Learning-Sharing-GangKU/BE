package com.gangku.be.service.participation;

import static org.assertj.core.api.Assertions.*;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * participantCount 낙관적 락 동시성 검증 테스트.
 *
 * <p>두 개의 독립 EntityManager(= 두 개의 영속성 컨텍스트)가 같은 Gathering을 읽은 뒤 하나가 먼저 커밋하면, 나머지는
 * OptimisticLockException을 받아야 한다. 이를 통해 @Version 필드가 Lost Update를 막는지 결정론적으로 검증한다.
 *
 * <p>@DataJpaTest를 사용하므로 Redis/Mail/S3/AI 등 외부 인프라 빈 없이 JPA 레이어만 로드한다.
 */
@Tag("unit")
@DataJpaTest
class JoinParticipationConcurrencyTest {

    @Autowired private EntityManagerFactory emf;

    /**
     * Propagation.NOT_SUPPORTED: @DataJpaTest가 붙여주는 클래스 레벨 @Transactional을 이 메서드에서만 비활성화한다. 테스트
     * 내부에서 EntityTransaction을 직접 제어해야 두 트랜잭션의 커밋 순서를 직접 조정할 수 있기 때문이다.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void optimisticLock_secondCommit_throwsOptimisticLockException() {
        // ── 1. SETUP: 영속화 (별도 트랜잭션) ─────────────────────────────────
        Long gatheringId = persistTestGathering();

        // ── 2. 두 개의 독립 영속성 컨텍스트 생성 ────────────────────────────────
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        EntityTransaction tx1 = em1.getTransaction();
        EntityTransaction tx2 = em2.getTransaction();

        try {
            // ── 3. 두 트랜잭션 모두 같은 버전(version=0)으로 읽기 ──────────────────
            tx1.begin();
            Gathering g1 = em1.find(Gathering.class, gatheringId);

            tx2.begin();
            Gathering g2 = em2.find(Gathering.class, gatheringId);

            assertThat(g1.getVersion()).isZero();
            assertThat(g2.getVersion()).isZero();
            assertThat(g1.getParticipantCount()).isEqualTo(g2.getParticipantCount());

            // ── 4. 두 트랜잭션 모두 메모리에서 +1 ───────────────────────────────
            g1.increaseParticipantCount(); // 1 → 2 (version 0 → 1 예정)
            g2.increaseParticipantCount(); // 1 → 2 (동일한 stale 값)

            // ── 5. 첫 번째 커밋 성공 → version=1 ────────────────────────────────
            tx1.commit();

            // ── 6. 두 번째 커밋 실패 → version 불일치(expected 0, actual 1) ──────
            //
            // JPA 명세: 버전 불일치 시 EntityTransaction.commit()이 RollbackException을 던진다.
            // 그 원인(cause)이 OptimisticLockException이어야 한다.
            assertThatThrownBy(tx2::commit)
                    .isInstanceOf(RollbackException.class)
                    .cause()
                    .isInstanceOf(OptimisticLockException.class);

        } finally {
            if (tx1.isActive()) tx1.rollback();
            if (tx2.isActive()) tx2.rollback();
            em1.close();
            em2.close();
        }

        // ── 7. 최종 상태 검증: 갱신이 정확히 1번만 반영됐는지 확인 ────────────────
        EntityManager verifyEm = emf.createEntityManager();
        try {
            Gathering saved = verifyEm.find(Gathering.class, gatheringId);
            assertThat(saved.getParticipantCount())
                    .as("두 요청 중 하나만 커밋되어야 하므로 초기값 + 1 이어야 한다")
                    .isEqualTo(2);
            assertThat(saved.getVersion()).as("버전이 정확히 1번 증가해야 한다").isEqualTo(1L);
        } finally {
            verifyEm.close();
        }
    }

    /** 테스트용 엔티티를 별도 트랜잭션에서 영속화하고 Gathering ID를 반환한다. */
    private Long persistTestGathering() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            User host =
                    User.create(
                            "host@concurrency.test", "encodedPw", "host", null, null, null, null);
            em.persist(host);

            Category cat = new Category();
            cat.setName("concurrency-test-cat");
            em.persist(cat);

            // capacity=3, participantCount=1(호스트) 로 시작하는 모임
            Gathering gathering =
                    Gathering.create(
                            host,
                            cat,
                            "동시성 테스트 모임",
                            "낙관적 락 검증용",
                            null,
                            3,
                            LocalDateTime.now().plusDays(1),
                            "서울",
                            "openchat-concurrency-" + System.nanoTime());
            em.persist(gathering);

            tx.commit();
            return gathering.getId();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
