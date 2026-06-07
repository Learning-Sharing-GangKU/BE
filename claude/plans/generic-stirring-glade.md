# participantCount 동시성 버그 수정 (낙관적 락)

## Context

`.claude/plans/improvement-analysis.md`의 우선순위 1위 버그를 수정한다.

**문제**: `ParticipationService.joinParticipation()`(line 47)에서 `gathering.increaseParticipantCount()`는
"읽기 → 메모리에서 +1 → flush" 순으로 동작한다. 두 사용자가 동시에 참가 신청하면 두 트랜잭션이
같은 `participantCount` 값을 읽고 각각 +1 하여 한쪽 갱신이 사라지는 **lost update**가 발생한다.
결과적으로 **정원 초과 참가가 허용**되고 통계가 틀어진다. 같은 취약점이
`cancelParticipation()` → `Participation.withdraw()` → `decreaseParticipantCount()` 경로에도 존재한다.

**접근**: `Gathering` 엔티티에 JPA 낙관적 락(`@Version`)을 도입한다. 충돌 시 서버는 재시도하지 않고
`409 CONFLICT`를 반환하며, 클라이언트가 재요청한다(사용자 선택). spring-retry 의존성은 추가하지 않는다.

**핵심 사실 (조사 결과)**:
- 프로젝트에 `@Version` 사용 엔티티가 전무 → 이번이 첫 도입.
- `participantCount` 변경 지점: `ParticipationService.joinParticipation()`(join), `Participation.withdraw()`(cancel), `UserService.deleteUser()`(line 107, 회원탈퇴) 세 곳. 모두 JPA dirty-checking에 의존(명시적 save 없음).
- `GlobalExceptionHandler`는 현재 `CustomException` / `MethodArgumentNotValidException` / `ConstraintViolationException`만 처리 → 낙관적 락 실패 핸들러 없음.
- local/prod 모두 `ddl-auto: update` → `version` 컬럼 자동 생성.

## 변경 사항

### 1. `Gathering` 엔티티에 `@Version` 추가
`src/main/java/com/gangku/be/domain/Gathering.java`
- `id` 필드 아래에 낙관적 락 버전 필드 추가:
  ```java
  @Version
  private Long version;
  ```
- `@Builder` / `@AllArgsConstructor`가 이미 있으므로 컴파일은 문제없음. Hibernate가 버전을 관리하므로
  `@Builder.Default`는 불필요(신규 엔티티는 0으로 시작).
- `increaseParticipantCount()` / `decreaseParticipantCount()` 로직은 **변경 없음** — flush 시점에
  Hibernate가 `UPDATE ... WHERE id=? AND version=?`로 버전을 검사하고, 영향 행이 0이면
  `ObjectOptimisticLockingFailureException`을 던진다.

### 2. 참가 충돌 에러코드 추가
`src/main/java/com/gangku/be/exception/constant/ParticipationErrorCode.java`
- 기존 패턴(`(code, message, HttpStatus.XXX.value())`, 한글 메시지)을 그대로 따라 항목 추가:
  ```java
  CONCURRENT_JOIN("CONCURRENT_JOIN", "다른 사용자의 요청과 충돌했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.CONFLICT.value());
  ```
  (현재 낙관적 락의 유일한 발생원이 참가 join/cancel이므로 `ParticipationErrorCode`에 둔다.)

### 3. 낙관적 락 예외 핸들러 추가
`src/main/java/com/gangku/be/exception/GlobalExceptionHandler.java`
- 기존 세 핸들러와 동일한 형태로 추가:
  ```java
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponseDto> handleOptimisticLock(
          ObjectOptimisticLockingFailureException e) {
      ErrorCode code = ParticipationErrorCode.CONCURRENT_JOIN;
      ErrorResponseDto body = ErrorResponseDto.of(code.getCode(), code.getMessage());
      return ResponseEntity.status(code.getStatus()).body(body);
  }
  ```
- import: `org.springframework.orm.ObjectOptimisticLockingFailureException`.

> `ParticipationService` / `Participation` / `Repository`는 **수정하지 않는다** — JPA가 버전 검사를
> 자동 수행하므로 서비스 로직 변경이 불필요하다.

## 범위 밖(이번 PR에서 제외, 후속 과제로 기록)
- **`UserService.deleteUser()`(line 95)에 `@Transactional` 누락**: `decreaseParticipantCount()`를
  호출하는데 메서드에 트랜잭션 선언이 없다(OSIV로 flush되는 구조). `@Version` 도입 후에도 단일 사용자
  탈퇴는 동시성 충돌이 드물어 정상 동작하나, 트랜잭션 경계 정리는 analysis 문서의 #3(N+1)과 함께 별도 처리한다.
- 기존 데이터 `version=NULL` 백필: `ddl-auto: update`는 컬럼만 추가하고 기존 행은 NULL로 남긴다.
  Hibernate는 NULL 버전 행을 첫 갱신 시 정상 처리하지만, 운영 배포 시 `UPDATE gatherings SET version = 0 WHERE version IS NULL;`
  실행을 권장(배포 체크리스트 항목).

## 테스트: 동시성 통합 테스트
신규 파일: `src/test/java/com/gangku/be/service/participation/JoinParticipationConcurrencyTest.java`

목적: 동시에 다수가 참가 신청해도 `participantCount`가 `capacity`를 **절대 초과하지 않음**을 검증.

설계 (인프라 의존 최소화):
- `@SpringBootTest(webEnvironment = NONE)` + `@Tag("integration")` + `@ActiveProfiles("test")`.
- 신규 `src/test/resources/application-test.yml`: H2 in-memory(`jdbc:h2:mem:...;DB_CLOSE_DELAY=-1`) +
  `ddl-auto: create-drop` + `H2Dialect`. `application.yml`의 `${...}` placeholder(MAIL/JWT/S3/CDN)에
  더미 기본값 제공하여 컨텍스트 로드 성공시킴.
- 외부 인프라 빈은 `@MockBean` 처리: Redis(`StringRedisTemplate`/`RedisConnectionFactory`),
  `JavaMailSender`, S3 클라이언트, `AiApiClient`. (참가 join 경로는 이들을 호출하지 않음.)
- 시나리오:
  1. host 1명 + guest용 user 10명, `capacity` 작은 모임(예: 초기 count=1, capacity=3) 저장.
  2. `ExecutorService`(10 threads) + `CountDownLatch`로 10명이 동시에
     `participationService.joinParticipation(gatheringId, userId)` 호출.
  3. 각 스레드 결과를 success / `ObjectOptimisticLockingFailureException`(=CONFLICT) / 기타로 집계.
- 검증:
  - 최종 `participantCount <= capacity` (정원 초과 없음 — 핵심).
  - `participantCount == 초기값 + successCount`.
  - `successCount + conflictCount == 10` (모든 요청이 성공 또는 충돌로 귀결).
  - `successCount >= 1` (최소 한 명은 진입).
  - DB의 `participation` 행 수 == successCount.
- 주의: 서비스 직접 호출이므로 잡히는 예외는 핸들러가 매핑한 409가 아니라 커밋 시점의
  `ObjectOptimisticLockingFailureException`이다. 409 매핑은 핸들러 단위의 관심사로 분리.

> 대안(폴백): 풀 컨텍스트 로드가 외부 인프라로 불안정하면, `@DataJpaTest` + `EntityManagerFactory`로
> 두 개의 독립 영속성 컨텍스트가 같은 버전을 읽고 순차 커밋해 두 번째에서
> `OptimisticLockException`이 발생함을 결정론적으로 검증하는 방식으로 대체.

## 검증 방법 (End-to-End)
1. 컴파일 + 포맷: `./gradlew spotlessApply compileJava`
2. 동시성 테스트: `./gradlew test --tests '*JoinParticipationConcurrencyTest'`
   (integration 태그 제외 설정이 있으면 `-PincludeTags=integration`으로 포함).
3. 전체 단위 테스트 회귀: `./gradlew test -PincludeTags=unit` — 기존 테스트 깨짐 없는지 확인.
4. (선택) 로컬 수동 검증: 앱 기동 후 동일 모임에 동시 참가 요청 2건을 보내면 하나는 `201`,
   다른 하나는 `409 CONCURRENT_JOIN`을 받고, `participant_count`가 정확히 1만 증가하는지 확인.

## 영향받는 파일 요약
| 파일 | 변경 |
|------|------|
| `domain/Gathering.java` | `@Version Long version` 필드 추가 |
| `exception/constant/ParticipationErrorCode.java` | `CONCURRENT_JOIN` 에러코드 추가 |
| `exception/GlobalExceptionHandler.java` | `ObjectOptimisticLockingFailureException` 핸들러 추가 |
| `test/.../participation/JoinParticipationConcurrencyTest.java` | 신규 동시성 테스트 |
| `test/resources/application-test.yml` | 신규 H2 테스트 프로필 |
