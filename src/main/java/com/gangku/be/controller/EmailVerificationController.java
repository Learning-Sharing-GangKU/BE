package com.gangku.be.controller;

import com.gangku.be.service.EmailVerificationRedisJwtService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/email/verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationRedisJwtService service;
    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.from}") private String from;
    @Value("${app.base-url}") private String baseUrl;

    @Getter
    static class SendReq {
        @NotBlank @Email @Size(min = 5, max = 254)
        private String email;
    }

    // 1) 발송
    @PostMapping
    public ResponseEntity<?> send(@RequestBody SendReq req, HttpServletResponse res) {
        try {
            EmailVerificationRedisJwtService.SendResult r = service.send(req.getEmail());

            ResponseCookie cookie = ResponseCookie.from("signup_session", r.sessionId())
                    .httpOnly(true).secure(true).sameSite("Strict").path("/")
                    .maxAge(r.sessionTtlMin() * 60).build();
            res.addHeader("Set-Cookie", cookie.toString());

            String verifyLink = baseUrl + "/api/v1/auth/email/verification/start?token=" + r.evJwt();
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(req.getEmail());
            msg.setSubject("[GangKU] 이메일 인증을 완료해주세요");
            msg.setText("""
                    안녕하세요, GangKU 입니다.
                    아래 링크를 클릭하면 이메일 인증이 완료됩니다.

                    %s

                    (유효시간: 10분)
                    """.formatted(verifyLink));
            mailSender.send(msg);

            return ResponseEntity.ok(Map.of("message","인증 이메일이 성공적으로 발송되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", Map.of("code","INVALID_EMAIL_FORMAT","message","이메일 형식이 올바르지 않습니다.")
            ));
        } catch (EmailVerificationRedisJwtService.EmailConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", Map.of("code","EMAIL_CONFLICT","message","이미 가입된 이메일이 있습니다.")
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", Map.of("code","INTERNAL_SERVER_ERROR","message","서버 오류로 인해 이메일을 전송하지 못했습니다. 잠시 후 다시 시도해주세요.")
            ));
        }
    }

    // 2) 외부 브라우저 클릭
    @GetMapping("/start")
    public ResponseEntity<?> start(@RequestParam("token") String token) {
        try {
            service.consume(token);
            return ResponseEntity.noContent().build(); // 204
        } catch (EmailVerificationRedisJwtService.InvalidTokenFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", Map.of("code","INVALID_TOKEN_FORMAT","message","유효하지 않은 토큰 형식입니다.")
            ));
        } catch (EmailVerificationRedisJwtService.TokenExpiredOrUsedException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                    "error", Map.of("code","TOKEN_EXPIRED","message","이메일 인증 토큰이 만료되었습니다.")
            ));
        }
    }

    // 3) 확인
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@CookieValue(value = "signup_session", required = false) String sid) {
        try {
            if (sid == null || sid.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", Map.of("code","INVALID_SESSION","message","유효한 가입 세션이 없습니다.")
                ));
            }
            EmailVerificationRedisJwtService.ConfirmResult r = service.confirm(sid);
            return ResponseEntity.ok(Map.of("verified", r.verified(), "email", r.email()));
        } catch (EmailVerificationRedisJwtService.InvalidSessionException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", Map.of("code","INVALID_SESSION","message","유효한 가입 세션이 없습니다.")
            ));
        } catch (EmailVerificationRedisJwtService.VerificationNotStartedException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", Map.of("code","VERIFICATION_NOT_STARTED","message","인증 메일 발송 기록이 없습니다.")
            ));
        } catch (EmailVerificationRedisJwtService.EmailMismatchException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", Map.of("code","EMAIL_MISMATCH","message","세션의 이메일과 인증된 이메일이 일치하지 않습니다.")
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", Map.of("code","INTERNAL_SERVER_ERROR","message","이메일 인증 확인 중 오류가 발생했습니다.")
            ));
        }
    }
}
