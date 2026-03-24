package com.gangku.be.service;

import com.gangku.be.config.auth.EmailVerificationProps;
import com.gangku.be.constant.auth.EmailConstants;
import com.gangku.be.constant.auth.TokenProperty;
import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.User;
import com.gangku.be.dto.auth.LoginRequestDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.model.auth.EmailVerificationConfirmResult;
import com.gangku.be.model.auth.EmailVerificationSendResult;
import com.gangku.be.model.auth.TokenPair;
import com.gangku.be.model.common.PrefixedId;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.util.jwt.EmailVerificationJwt;
import com.gangku.be.util.jwt.EmailVerificationJwt.EmailVerificationToken;
import com.gangku.be.util.jwt.JwtTokenProvider;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailVerificationJwt emailVerificationJwt;
    private final EmailVerificationProps emailVerificationProps;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.email.from}")
    private String fromEmailAddress;

    private final JavaMailSender javaMailSender;

    private static final String REDIS_CONSUME_EMAIL_BY_TOKEN_SCRIPT =
            """
      local key = KEYS[1]
      local value = redis.call('GET', key)
      if value then
        redis.call('DEL', key)
        return value
      else
        return nil
      end
    """;

    @Transactional
    public TokenPair login(LoginRequestDto loginRequestDto) {
        User user =
                findUserByEmailAndPassword(
                        loginRequestDto.getEmail(), loginRequestDto.getPassword());

        String publicUserId = PrefixedId.of(ResourceType.USER, user.getId()).toExternal();

        TokenPair tokenPair = generateToken(publicUserId);

        updateRefreshToken(
                user, tokenPair.refreshToken(), TokenProperty.REFRESH_TOKEN.getExpirationInDays());

        return tokenPair;
    }

    @Transactional
    public TokenPair reIssue(HttpServletRequest request) {
        String refreshToken = findRefreshTokenFromCookie(request);
        User user = findUserFromRefreshToken(refreshToken);
        verifyRefreshToken(user, refreshToken);

        String publicUserId = PrefixedId.of(ResourceType.USER, user.getId()).toExternal();

        TokenPair tokenPair = generateToken(publicUserId);

        updateRefreshToken(
                user, tokenPair.refreshToken(), TokenProperty.REFRESH_TOKEN.getExpirationInDays());

        return tokenPair;
    }

    @Transactional
    public void logout(HttpServletRequest request) {
        String refreshToken = findRefreshTokenFromCookie(request);
        User user = findUserFromRefreshToken(refreshToken);
        verifyRefreshToken(user, refreshToken);

        updateRefreshToken(user, refreshToken, 0L);
    }

    @Transactional
    public EmailVerificationSendResult sendEmailVerification(String email) {
        verifyEmailConflict(email);

        EmailVerificationToken emailVerificationToken =
                emailVerificationJwt.generateToken(
                        email, Duration.ofMinutes(emailVerificationProps.getTokenTtlMinutes()));

        String sessionId = createSignUpSessionAndWhiteListJti(email, emailVerificationToken);

        sendEmail(email, emailVerificationToken);

        return new EmailVerificationSendResult(
                sessionId,
                emailVerificationToken.token(),
                emailVerificationProps.getSessionTtlMinutes());
    }

    @Transactional
    public void consumeEmailVerification(String emailVerificationTokenString) {
        String tokenId = extractTokenIdFromVerifiedJwt(emailVerificationTokenString);
        String email = consumeEmailByTokenId(tokenId);
        markEmailAsVerified(email);
    }

    @Transactional
    public EmailVerificationConfirmResult confirmEmailVerification(String sessionId) {
        String sessionKey = requireValidSessionKey(sessionId);
        String sessionEmail = resolveEmailFromSession(sessionKey);

        ensureEmailVerificationCompleted(sessionEmail);
        markSessionAsVerified(sessionKey);

        return new EmailVerificationConfirmResult(true, sessionEmail);
    }

    private TokenPair generateToken(String userId) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        return new TokenPair(accessToken, refreshToken);
    }

    private void updateRefreshToken(User user, String refreshToken, Long plusDays) {
        if (plusDays <= 0) {
            user.clearRefreshToken();
        } else {
            user.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(plusDays));
        }
        userRepository.save(user);
    }

    private String createSignUpSessionAndWhiteListJti(
            String email, EmailVerificationToken emailVerificationToken) {

        Duration timeToLive = Duration.between(Instant.now(), emailVerificationToken.expiresAt());

        stringRedisTemplate
                .opsForValue()
                .set(
                        emailVerificationTokenKey(emailVerificationToken.tokenId()),
                        email,
                        timeToLive);

        String sessionId = UUID.randomUUID().toString();
        String sessionKey = signupSessionKey(sessionId);

        stringRedisTemplate.opsForHash().put(sessionKey, "email", email);
        stringRedisTemplate.opsForHash().put(sessionKey, "verified", "0");
        stringRedisTemplate.expire(
                sessionKey, Duration.ofMinutes(emailVerificationProps.getSessionTtlMinutes()));

        return sessionId;
    }

    private void sendEmail(String email, EmailVerificationToken emailVerificationToken) {
        String verificationUrl =
                baseUrl + EmailConstants.VERIFICATION_PATH + emailVerificationToken.token();

        String emailBody =
                EmailConstants.VERIFICATION_BODY_TEMPLATE.formatted(
                        verificationUrl, emailVerificationProps.getSessionTtlMinutes());

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmailAddress);
        mailMessage.setTo(email);
        mailMessage.setSubject(EmailConstants.VERIFICATION_SUBJECT);
        mailMessage.setText(emailBody);

        javaMailSender.send(mailMessage);
    }

    private void markEmailAsVerified(String email) {
        Duration timeToLive = Duration.ofMinutes(emailVerificationProps.getTokenTtlMinutes());
        stringRedisTemplate.opsForValue().set(verifiedEmailKey(email), "1", timeToLive);
    }

    private String consumeEmailByTokenId(String tokenId) {
        DefaultRedisScript<String> script =
                new DefaultRedisScript<>(REDIS_CONSUME_EMAIL_BY_TOKEN_SCRIPT, String.class);

        String email =
                stringRedisTemplate.execute(script, List.of(emailVerificationTokenKey(tokenId)));

        if (email == null) {
            throw new CustomException(AuthErrorCode.EMAIL_TOKEN_EXPIRED);
        }

        return email;
    }

    private String extractTokenIdFromVerifiedJwt(String emailVerificationTokenString) {
        Jws<Claims> parsedToken = emailVerificationJwt.parseClaims(emailVerificationTokenString);
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

    private User findUserByEmailAndPassword(String email, String rawPassword) {
        return userRepository
                .findByEmail(email)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPassword()))
                .orElseThrow(() -> new CustomException(UserErrorCode.INVALID_CREDENTIAL));
    }

    private User findUserFromRefreshToken(String refreshToken) {
        Long userId = jwtTokenProvider.extractUserIdFromRefreshToken(refreshToken);

        return userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private String findRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        return Arrays.stream(cookies)
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElseThrow(() -> new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    private void verifyRefreshToken(User user, String refreshToken) {
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new CustomException(AuthErrorCode.TOKEN_MISMATCH);
        }
    }

    private void verifyEmailConflict(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CustomException(UserErrorCode.EMAIL_CONFLICT);
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
        Object emailValue = stringRedisTemplate.opsForHash().get(sessionKey, "email");

        if (emailValue == null) {
            throw new CustomException(AuthErrorCode.INVALID_SESSION);
        }

        return String.valueOf(emailValue);
    }

    private void ensureEmailVerificationCompleted(String email) {
        String verifiedEmailFlag = stringRedisTemplate.opsForValue().get(verifiedEmailKey(email));

        if (verifiedEmailFlag == null) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private void markSessionAsVerified(String sessionKey) {
        stringRedisTemplate.opsForHash().put(sessionKey, "verified", "1");
    }
}
