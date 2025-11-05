package com.gangku.be.service;

import com.gangku.be.config.redis.EmailVerificationProps;
import com.gangku.be.jwt.EmailVerificationJwt;
import com.gangku.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationRedisJwtService {

    private final StringRedisTemplate rt;
    private final EmailVerificationJwt jwt;
    private final EmailVerificationProps props;
    private final UserRepository userRepo;

    private static String jtiKey(String jti) { return "auth:signup:jti:" + jti; }
    private static String sessKey(String sid) { return "auth:signup:session:" + sid; }
    private static String verifiedEmailKey(String email) { return "auth:signup:verified-email:" + email; }

    /**
     * jti 키에서 이메일 값을 읽고 삭제까지 원자적으로 처리.
     * 존재하면 이메일을, 없으면 nil을 반환.
     */
    private static final String CONSUME_GET_EMAIL_SCRIPT = """
      local key = KEYS[1]
      local v = redis.call('GET', key)
      if v then
        redis.call('DEL', key)
        return v
      else
        return nil
      end
    """;

    public static class SendResult {
        private final String sessionId;
        private final String evJwt;
        private final long sessionTtlMin;
        public SendResult(String sessionId, String evJwt, long sessionTtlMin) {
            this.sessionId = sessionId;
            this.evJwt = evJwt;
            this.sessionTtlMin = sessionTtlMin;
        }
        public String sessionId() { return sessionId; }
        public String evJwt() { return evJwt; }
        public long sessionTtlMin() { return sessionTtlMin; }
    }

    public SendResult send(String email) {
        if (email == null || !email.toLowerCase().endsWith("@konkuk.ac.kr")) {
            throw new IllegalArgumentException("INVALID_EMAIL_FORMAT");
        }
        if (userRepo.findByEmail(email).isPresent()) {
            throw new EmailConflictException();
        }

        EmailVerificationJwt.EvJwt ev =
                jwt.create(email, Duration.ofMinutes(props.getTokenTtlMinutes()));

        // jti 화이트리스트 (만료와 동일 TTL)
        Duration ttl = Duration.between(Instant.now(), ev.exp());
        rt.opsForValue().set(jtiKey(ev.jti()), email, ttl);

        // 가입 세션 (verified=0)
        String sid = UUID.randomUUID().toString();
        rt.opsForHash().put(sessKey(sid), "email", email);
        rt.opsForHash().put(sessKey(sid), "verified", "0");
        rt.expire(sessKey(sid), Duration.ofMinutes(props.getSessionTtlMinutes()));

        return new SendResult(sid, ev.token(), props.getSessionTtlMinutes());
    }

    /** 외부 브라우저에서 이메일 내 링크 클릭 */
    public void consume(String evJwt) {
        // 1) JWT 서명/만료 검증
        io.jsonwebtoken.Jws<io.jsonwebtoken.Claims> parsed = jwt.parse(evJwt);
        if (!"signup".equals(parsed.getBody().getAudience())) {
            throw new InvalidTokenFormatException();
        }
        String jti = parsed.getBody().getId();

        // 2) jti 화이트리스트 원자 소비 → 이메일 획득
        DefaultRedisScript<String> script = new DefaultRedisScript<>(CONSUME_GET_EMAIL_SCRIPT, String.class);
        String email = rt.execute(script, List.of(jtiKey(jti)));
        if (email == null) {
            // 없거나 이미 사용/만료
            throw new TokenExpiredOrUsedException();
        }

        // 3) 이 이메일이 인증됨을 표시 (세션과 분리된, 이메일 단위 플래그)
        //    TTL은 세션 TTL과 동일하게 부여 (확인 단계에서 TTL 내에만 유효)
        rt.opsForValue().set(verifiedEmailKey(email), "1",
                Duration.ofMinutes(props.getSessionTtlMinutes()));
    }

    /** 확인 단계: 세션의 email과 '인증됨 플래그'를 대조 */
    public ConfirmResult confirm(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new InvalidSessionException();
        }
        String key = sessKey(sessionId);

        Object emailObj = rt.opsForHash().get(key, "email");
        if (emailObj == null) {
            throw new InvalidSessionException();
        }
        String sessionEmail = String.valueOf(emailObj);

        // 이메일 인증(링크 클릭) 플래그가 있어야 함
        String verifiedFlag = rt.opsForValue().get(verifiedEmailKey(sessionEmail));
        if (verifiedFlag == null) {
            // 링크를 누르지 않았거나, 토큰이 만료/소비되지 않음
            throw new VerificationNotStartedException();
        }

        // 세션에 verified=1 마킹
        rt.opsForHash().put(key, "verified", "1");
        // 인증 플래그는 소비(정책에 따라 유지도 가능)
        rt.delete(verifiedEmailKey(sessionEmail));

        return new ConfirmResult(true, sessionEmail);
    }

    public static class ConfirmResult {
        private final boolean verified;
        private final String email;
        public ConfirmResult(boolean verified, String email) {
            this.verified = verified; this.email = email;
        }
        public boolean verified() { return verified; }
        public String email() { return email; }
    }

    // 예외
    public static class EmailConflictException extends RuntimeException {}
    public static class InvalidTokenFormatException extends RuntimeException {}
    public static class TokenExpiredOrUsedException extends RuntimeException {}
    public static class InvalidSessionException extends RuntimeException {}
    public static class VerificationNotStartedException extends RuntimeException {}
    public static class EmailMismatchException extends RuntimeException {}
}
