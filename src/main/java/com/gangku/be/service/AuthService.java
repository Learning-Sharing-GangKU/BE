package com.gangku.be.service;

import com.gangku.be.config.auth.EmailVerificationProps;
import com.gangku.be.constant.auth.EmailConstants;
import com.gangku.be.constant.auth.TokenProperty;
import com.gangku.be.domain.User;
import com.gangku.be.dto.auth.LoginRequestDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.model.EmailVerificationSendResult;
import com.gangku.be.model.EmailVerificationConfirmResult;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.util.jwt.EmailVerificationJwt;
import com.gangku.be.util.jwt.EmailVerificationJwt.EmailVerificationToken;
import com.gangku.be.util.jwt.JwtTokenProvider;
import com.gangku.be.model.TokenPair;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // --- 이메일 인증 관련 의존성 ---
    private final StringRedisTemplate stringRedisTemplate;
    private final EmailVerificationJwt emailVerificationJwt;
    private final EmailVerificationProps emailVerificationProps;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.email.from}")
    private String fromEmailAddress;

    // --- 메일 발송 ---
    private final JavaMailSender javaMailSender;

    private static final String REDIS_CONSUME_EMAIL_BY_TOKEN_SCRIPT = """
      local key = KEYS[1]
      local value = redis.call('GET', key)
      if value then
        redis.call('DEL', key)
        return value
      else
        return nil
      end
    """;

    // ======================================================
    // 1. 로그인 / 토큰 재발급 / 로그아웃
    // ======================================================

    public TokenPair login(LoginRequestDto loginRequestDto) {

        // 1) 이메일 / 비밀번호 검증 및 유저 반환
        User user = findUserByEmailAndPassword(loginRequestDto.getEmail(), loginRequestDto.getPassword());

        // 2) Token 생성
        TokenPair tokenPair = generateToken(user.getId());

        // 3) Refresh Token DB에 갱신
        updateRefreshToken(user, tokenPair.refreshToken(), TokenProperty.REFRESH_TOKEN.getExpirationInDays());

        return tokenPair;
    }

    public TokenPair reIssue(HttpServletRequest request) {

        // 1) 쿠키에서 Refresh Token 꺼내기
        String refreshToken = findRefreshTokenFromCookie(request);

        // 2) Refresh Token 기본 검증
        validateRefreshToken(refreshToken);

        // 3) Refresh Token으로부터 User 정보 가져오기
        User user = findUserFromRefreshToken(refreshToken);

        // 5) DB의 Refresh Token와 검증
        verifyRefreshToken(user, refreshToken);

        // 6) 토큰 회전 - 새 Access + 새 Refresh Token 발급
        TokenPair tokenPair = generateToken(user.getId());

        // 7) DB에 발급한 새 Refresh Token 저장
        updateRefreshToken(user, tokenPair.refreshToken(), TokenProperty.REFRESH_TOKEN.getExpirationInDays());

        return tokenPair;
    }

    public void logout(HttpServletRequest request) {

        // 1) 쿠키에서 Refresh Token 꺼내기
        String refreshToken = findRefreshTokenFromCookie(request);

        // 2) 토큰에서 userId 추출 및 DB에서 유저 조회
        User user = findUserFromRefreshToken(refreshToken);

        // 3) Refresh Token 기본 검증
        verifyRefreshToken(user, refreshToken);

        // 3. DB에 저장된 리프레시 토큰 제거
        updateRefreshToken(user, refreshToken, 0L);
    }


    // ======================================================
    // 2. 이메일 인증(회원가입) 플로우
    //    - STEP1: 인증 메일 발송 (sendEmailVerification)
    //    - STEP2: 이메일 내 링크 클릭 (consumeEmailVerification)
    //    - STEP3: 클라이언트에서 세션 확인 (confirmEmailVerification)
    // ======================================================

    public EmailVerificationSendResult sendEmailVerification(String email) {

        // 1) 이메일 검증
        validateEmailFormat(email);
        verifyEmailConflict(email);

        // 2) 이메일 인증용 JWT 생성
        EmailVerificationToken emailVerificationToken =
                emailVerificationJwt.createToken(email,
                        Duration.ofMinutes(emailVerificationProps.getTokenTtlMinutes()));

        // 3) 세션 정보 저장 + Redis에 JTI 화이트리스트 등록
        String sessionId = createSignUpSessionAndWhiteListJti(email, emailVerificationToken);

        // 4) 인증 메일 발송 (Service 책임)
        sendEmail(email, emailVerificationToken);

        return new EmailVerificationSendResult(
                sessionId,
                emailVerificationToken.token(),
                emailVerificationProps.getSessionTtlMinutes()
        );
    }

    public void consumeEmailVerification(String emailVerificationTokenString) {

        // 1) JWT 서명/만료 검증
        String tokenId = extractTokenIdFromVerifiedJwt(emailVerificationTokenString);

        // 2) jti 화이트리스트 원자 소비 → 이메일 획득
        String email = consumeEmailByTokenId(tokenId);

        // 3) 이 이메일이 인증됨을 표시 (세션과 분리된, 이메일 단위 플래그)
        markEmailAsVerified(email);
    }

    public EmailVerificationConfirmResult confirmEmailVerification(String sessionId) {

        // 1) 세션 ID 검증 + Redis 키 획득
        String sessionKey = requireValidSessionKey(sessionId);

        // 2) 세션에서 이메일 가져오기
        String sessionEmail = resolveEmailFromSession(sessionKey);

        // 3) 이 이메일이 실제로 “인증 완료” 상태인지 확인
        ensureEmailVerificationCompleted(sessionEmail);

        // 4) 세션에 verified=1 마킹
        markSessionAsVerified(sessionKey);

        // 5) 이메일 인증 플래그는 1회용으로 소비
        consumeVerifiedEmailFlag(sessionEmail);

        return new EmailVerificationConfirmResult(true, sessionEmail);
    }

    /**
     * --- 비즈니스 로직 헬퍼 메서드 ---
     */

    private TokenPair generateToken(Long userId) {

        String accessToken = jwtTokenProvider.generateAccessToken(String.valueOf(userId));
        String refreshToken = jwtTokenProvider.generateRefreshToken(String.valueOf(userId));

        return new TokenPair(accessToken, refreshToken);
    }

    private void updateRefreshToken(User user, String refreshToken, Long plusDays) {

        if (plusDays <= 0) {
            user.clearRefreshToken();
        }
        else {
            user.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(plusDays));
        }
        userRepository.save(user);
    }

    private String createSignUpSessionAndWhiteListJti(String email, EmailVerificationToken emailVerificationToken) {
        Duration timeToLive = Duration.between(Instant.now(), emailVerificationToken.expiresAt());
        stringRedisTemplate.opsForValue().set(
                emailVerificationTokenKey(emailVerificationToken.tokenId()),
                email,
                timeToLive
        );

        String sessionId = UUID.randomUUID().toString();
        String sessionKey = signupSessionKey(sessionId);
        stringRedisTemplate.opsForHash().put(sessionKey, "email", email);
        stringRedisTemplate.opsForHash().put(sessionKey, "verified", "0");
        stringRedisTemplate.expire(
                sessionKey,
                Duration.ofMinutes(emailVerificationProps.getSessionTtlMinutes())
        );
        return sessionId;
    }

    private void sendEmail(String email, EmailVerificationToken emailVerificationToken) {

        String verificationUrl = baseUrl + EmailConstants.VERIFICATION_PATH + emailVerificationToken.token();

        String emailBody = EmailConstants.VERIFICATION_BODY_TEMPLATE.formatted(
                verificationUrl,
                emailVerificationProps.getSessionTtlMinutes()
        );

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmailAddress);
        mailMessage.setTo(email);
        mailMessage.setSubject(EmailConstants.VERIFICATION_SUBJECT);
        mailMessage.setText(emailBody);

        javaMailSender.send(mailMessage);
    }

    private void markEmailAsVerified(String email) {
        Duration timeToLive = Duration.ofMinutes(emailVerificationProps.getTokenTtlMinutes());
        stringRedisTemplate
                .opsForValue()
                .set(verifiedEmailKey(email), "1", timeToLive);
    }

    private String consumeEmailByTokenId(String tokenId) {
        DefaultRedisScript<String> script =
                new DefaultRedisScript<>(REDIS_CONSUME_EMAIL_BY_TOKEN_SCRIPT, String.class);

        String email = stringRedisTemplate.execute(
                script,
                List.of(emailVerificationTokenKey(tokenId))
        );
        if (email == null) {
            throw new CustomException(AuthErrorCode.USER_NOT_FOUND_BY_TOKEN);
        }
        return email;
    }

    private String extractTokenIdFromVerifiedJwt(String emailVerificationTokenString) {
        Jws<Claims> parsedToken = emailVerificationJwt.parseToken(emailVerificationTokenString);
        return parsedToken.getBody().getId();
    }

    private static String emailVerificationTokenKey(String tokenId) {
        return "auth:signup:email-verification-token:" + tokenId;
    }

    private static String signupSessionKey(String sessionId) {
        return "auth:signup:session:" + sessionId;
    }

    private static String verifiedEmailKey(String email) {
        return "auth:signup:verified-email:" + email;
    }

    /**
     * --- 검증 및 반환 헬퍼 메서드 ---
     */

    private User findUserByEmailAndPassword(String email, String rawPassword) {

        return userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPassword()))
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_CREDENTIAL));
    }

    private User findUserFromRefreshToken(String refreshToken) {

        String userId = jwtTokenProvider.getSubject(refreshToken);

        return userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private String findRefreshTokenFromCookie(HttpServletRequest request) {

        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    private void validateRefreshToken(String refreshToken) {
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private void verifyRefreshToken(User user, String refreshToken) {
        if (user.getRefreshToken() == null ||
                !user.getRefreshToken().equals(refreshToken)) {
            throw new CustomException(AuthErrorCode.TOKEN_MISMATCH);
        }
    }

    private void verifyEmailConflict(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CustomException(AuthErrorCode.EMAIL_CONFLICT);
        }
    }

    private void validateEmailFormat(String email) {
        if (email == null || !email.toLowerCase().endsWith("@konkuk.ac.kr")) {
            throw new CustomException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    private String requireValidSessionKey(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new CustomException(AuthErrorCode.INVALID_SESSION);
        }

        String sessionKey = signupSessionKey(sessionId);
        Boolean exists = stringRedisTemplate.hasKey(sessionKey);
        if (exists == null || !exists) {
            throw new CustomException(AuthErrorCode.INVALID_SESSION);
        }

        return sessionKey;
    }

    private String resolveEmailFromSession(String sessionKey) {
        Object emailValue = stringRedisTemplate
                .opsForHash().get(sessionKey, "email");

        if (emailValue == null) {
            throw new CustomException(AuthErrorCode.INVALID_SESSION);
        }

        return String.valueOf(emailValue);
    }

    private void ensureEmailVerificationCompleted(String email) {
        String verifiedEmailFlag = stringRedisTemplate.opsForValue().get(verifiedEmailKey(email));

        if (verifiedEmailFlag == null) {
            throw new CustomException(AuthErrorCode.VERIFICATION_NOT_STARTED);
        }
    }

    private void markSessionAsVerified(String sessionKey) {
        stringRedisTemplate.opsForHash().put(sessionKey, "verified", "1");
    }

    private void consumeVerifiedEmailFlag(String email) {
        stringRedisTemplate.delete(verifiedEmailKey(email));
    }
}
