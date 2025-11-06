package com.gangku.be.service;

import com.gangku.be.config.s3.AppProps;
import com.gangku.be.config.s3.AssetPolicyProps;
import com.gangku.be.dto.object.PresignRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObjectStoragePresignService {

    private final S3Presigner presigner;
    private final AppProps app;
    private final AssetPolicyProps policy;

    public Result presign(PresignRequestDto req) {
        // 1) MIME major → category 선택 (현재는 이미지만 저장하지만, 확장성 고려)
        String[] parts = req.fileType().split("/");
        String major = parts[0].toLowerCase(Locale.ROOT);
        String type = switch (major) {
            case "image" -> "image";
            case "video" -> "video";
            default -> "file";
        };

        // 2) 허용 타입 검증
        boolean allowed = policy.getCategories().stream()
                .anyMatch(c -> c.getType().equalsIgnoreCase(type)
                        && c.getAllowedContentTypes().contains(req.fileType()));
        if (!allowed) {
            throw new BadRequestApiException("INVALID_FILE_TYPE", "허용되지 않는 파일 형식입니다.");
        }

        // 3) 키 생성: statics/{category}/{env}/yyyy/MM/uuid.ext
        String key = buildKey(type, req.fileName());

        // 4) PutObjectRequest
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(app.getS3().getBucket())
                .key(key)
                .contentType(req.fileType())
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .putObjectRequest(put)
                .signatureDuration(Duration.ofSeconds(app.getS3().getTtlSeconds()))
                .build();

        PresignedPutObjectRequest p = presigner.presignPutObject(presignReq);

        return new Result(
                key,
                p.url().toString(),
                toPublicUrl(key),
                app.getS3().getTtlSeconds()
        );
    }

    private String buildKey(String categoryPrefix, String fileName) {
        YearMonth ym = YearMonth.now(ZoneId.of("Asia/Seoul"));
        String ext = extOf(fileName);
        return String.format("%s/%s/%s/%04d/%02d/%s.%s",
                app.getS3().getBasePrefix(), categoryPrefix, app.getS3().getEnvPrefix(),
                ym.getYear(), ym.getMonthValue(), UUID.randomUUID(), ext
        );
    }

    private String toPublicUrl(String key) {
        String cdn = app.getCdn().getBaseUrl();
        if (cdn != null && !cdn.isBlank()) {
            return (cdn.endsWith("/") ? cdn : cdn + "/") + key;
        }
        return "https://" + app.getS3().getBucket() + ".s3." + app.getS3().getRegion() + ".amazonaws.com/" + key;
    }

    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return (i > -1 ? name.substring(i + 1) : "bin").toLowerCase(Locale.ROOT);
    }

    public record Result(String objectKey, String uploadUrl, String fileUrl, int expiresIn) {}

    public static class BadRequestApiException extends RuntimeException {
        public final String code;
        public BadRequestApiException(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
