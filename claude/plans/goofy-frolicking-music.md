# UserService.deleteUser() N+1 쿼리 해결

## Context

`UserService.deleteUser()`는 회원 탈퇴 시 사용자가 참여한 모든 모임의 참여 인원수(`participantCount`)를 감소시킨다. 현재 구현은 참여 목록을 조회한 뒤 루프를 돌며 LAZY 연관(`gathering`, `gathering.host`)에 접근하는데, 이 LAZY 필드들이 루프마다 추가 SELECT를 발생시켜 **2N + 1개**의 쿼리가 나간다 (N = 참여 모임 수). 참여 모임이 많은 사용자가 탈퇴하면 DB 부하와 응답 지연이 발생한다.

목표: `JOIN FETCH`로 `gathering`과 `host`를 한 번에 로딩하여 쿼리 수를 **1개**로 줄인다.

### 문제 코드 (`UserService.java:101-109`)
```java
List<Participation> participations = participationRepository.findAllByUser(user);  // 쿼리 1
for (Participation participation : participations) {
    Gathering gathering = participation.getGathering();          // LAZY → N번
    if (!gathering.getHost().getId().equals(user.getId())) {     // LAZY → N번
        gathering.decreaseParticipantCount();
    }
}
```

### 근본 원인
- `ParticipationRepository.findAllByUser()` (line 58): 기본 파생 쿼리, 페치 없음
- `Participation.gathering` (Participation.java:31): `@ManyToOne(fetch = LAZY)`
- `Gathering.host` (Gathering.java:31): `@ManyToOne(fetch = LAZY)`

### 사용처 조사 결과
`findAllByUser`는 **`deleteUser()` 한 곳에서만** 사용됨 (production). 테스트 `DeleteUserUnitTest`가 모킹 중. → 안전하게 교체 가능.

---

## 변경 사항

### 1. `ParticipationRepository.java` — JOIN FETCH 메서드로 교체

기존 `findAllByUser` (line 58)를 제거하고, 프로젝트의 기존 JOIN FETCH 컨벤션(`GatheringRepository.findByHostId` 참고, text block + `JOIN FETCH`)을 따라 새 메서드 추가:

```java
@Query("""
    SELECT p
    FROM Participation p
    JOIN FETCH p.gathering g
    JOIN FETCH g.host
    WHERE p.user = :user
""")
List<Participation> findAllByUserWithGatheringAndHost(@Param("user") User user);
```

> 참고: `gathering`, `host` 모두 `@ManyToOne`(단일 연관)이므로 컬렉션 페치가 아니다 → 페이징/카테시안 곱 문제 없음, `DISTINCT` 불필요.

### 2. `UserService.java` (line 101) — 새 메서드 호출로 변경

```java
List<Participation> participations =
    participationRepository.findAllByUserWithGatheringAndHost(user);
```

나머지 루프 로직은 그대로 둔다 (이제 `getGathering()` / `getHost()`가 이미 로딩된 상태라 추가 쿼리 없음).

### 3. `DeleteUserUnitTest.java` (line 42, 49) — 모킹 메서드 이름 갱신

```java
when(participationRepository.findAllByUserWithGatheringAndHost(user))
    .thenReturn(Collections.emptyList());
...
verify(participationRepository, times(1)).findAllByUserWithGatheringAndHost(user);
```

### 4. 신규 쿼리 카운트 테스트 추가 (N+1 실제 검증)

`src/test/java/com/gangku/be/repository/ParticipationRepositoryN1Test.java` (신규)

`@DataJpaTest` + Hibernate `Statistics`로 실제 쿼리 수를 검증한다. 기존 `JoinParticipationConcurrencyTest`의 `@DataJpaTest` + `EntityManagerFactory` 패턴과 엔티티 영속화 헬퍼(`User.create`, `Gathering.create`, `Category` setName)를 참고한다.

테스트 시나리오:
- host 1명 + 게스트 1명, 모임 여러 개(예: 3개) 생성, 게스트를 각 모임에 참여시킴
- 영속성 컨텍스트 clear 후 `findAllByUserWithGatheringAndHost(guest)` 호출
- 반환된 `Participation`들의 `getGathering().getTitle()`, `getGathering().getHost().getId()` 접근
- `SessionFactory`의 `Statistics.getPrepareStatementCount()`로 **쿼리 1개**임을 assert

```java
Statistics stats = em.unwrap(Session.class).getSessionFactory()
        .getStatistics();
stats.setStatisticsEnabled(true);
stats.clear();
// ... 조회 + 연관 접근 ...
assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
```

---

## 영향 범위

| 파일 | 변경 |
|------|------|
| `repository/ParticipationRepository.java` | `findAllByUser` → `findAllByUserWithGatheringAndHost` (JOIN FETCH) |
| `service/UserService.java` (L101) | 새 메서드 호출 |
| `test/.../user/DeleteUserUnitTest.java` (L42,49) | 모킹 메서드명 갱신 |
| `test/.../repository/ParticipationRepositoryN1Test.java` | 신규 쿼리 카운트 테스트 |

**개선 효과**: `2N + 1` → **1** 쿼리

---

## 검증 (Verification)

```bash
# 단위 테스트 (모킹 기반, 메서드명 갱신 확인)
./gradlew test --tests "com.gangku.be.service.user.DeleteUserUnitTest"

# N+1 실제 검증 (쿼리 카운트)
./gradlew test --tests "com.gangku.be.repository.ParticipationRepositoryN1Test"

# 전체 회귀
./gradlew test
```

- 쿼리 카운트 테스트가 `getPrepareStatementCount() == 1`로 통과하면 N+1 해결 확인
- (선택) `application.yml`/test profile에 `spring.jpa.show-sql=true` 또는 `org.hibernate.SQL=DEBUG` 로그로 실제 SQL 1줄만 나가는지 육안 확인

---

## 참고: 동일 패턴의 나머지 N+1 (이번 범위 아님)
`.claude/N+1-issues-summary.md`에 정리됨 — ClusteringService, ParticipationService.getParticipants(), UserService.getUserProfile(), ReviewService 순으로 후속 작업 예정.
