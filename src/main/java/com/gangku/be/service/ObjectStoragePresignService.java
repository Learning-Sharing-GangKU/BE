package com.gangku.be.service;

import com.gangku.be.config.aws.AssetPolicyProps;
import com.gangku.be.config.aws.AwsAppProps;
import com.gangku.be.dto.object.PresignRequestDto;
import com.gangku.be.dto.object.PresignResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.ObjectStorageErrorCode;
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

    private final S3Presigner s3Presigner;
    private final AwsAppProps awsAppProps;
    private final AssetPolicyProps assetPolicyProps;

    public PresignResponseDto presign(PresignRequestDto presignRequestDto) {

        // 1) MIME major → category 선택 (현재는 이미지만 저장하지만, 확장성 고려)
        String fileType = getFileTypeFromRequest(presignRequestDto);

        // 2) 허용 타입 검증
        validateFileType(presignRequestDto, fileType);

        // 3) 키 생성: statics/{category}/{env}/yyyy/MM/uuid.ext
        String key = buildKey(fileType, presignRequestDto.getFileName());

        // 4) PutObjectRequest
        PresignedPutObjectRequest presignedPutObjectRequest = generatePresignedPutUrl(
                presignRequestDto, key);

        return new PresignResponseDto(
                key,
                presignedPutObjectRequest.url().toString(),
                toPublicUrl(key),
                awsAppProps.getS3().getTtlSeconds()
        );
    }

    private String getFileTypeFromRequest(PresignRequestDto presignRequestDto) {
        String[] parts = presignRequestDto.getFileType().split("/");
        String major = parts[0].toLowerCase(Locale.ROOT);
        return switch (major) {
            case "image" -> "image";
            case "video" -> "video";
            default -> "file";
        };
    }

    private void validateFileType(PresignRequestDto presignRequestDto, String fileType) {
        assetPolicyProps.getCategories().stream()
                .filter(c -> c.getType().equalsIgnoreCase(fileType)
                        && c.getAllowedContentTypes().contains(presignRequestDto.getFileType()))
                .findAny()
                .orElseThrow(() -> new CustomException(ObjectStorageErrorCode.INVALID_FILE_TYPE));
    }

    private PresignedPutObjectRequest generatePresignedPutUrl(PresignRequestDto presignRequestDto, String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsAppProps.getS3().getBucket())
                .key(key)
                .contentType(presignRequestDto.getFileType())
                .build();

        PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofSeconds(awsAppProps.getS3().getTtlSeconds()))
                .build();

        return s3Presigner.presignPutObject(putObjectPresignRequest);
    }

    private String toPublicUrl(String key) {
        String cdn = awsAppProps.getCdn().getBaseUrl();
        if (cdn != null && !cdn.isBlank()) {
            return (cdn.endsWith("/") ? cdn : cdn + "/") + key;
        }
        return "https://"
                + awsAppProps.getS3().getBucket()
                + ".s3." + awsAppProps.getS3().getRegion()
                + ".amazonaws.com/"
                + key;
    }

    private String buildKey(String categoryPrefix, String fileName) {
        YearMonth ym = YearMonth.now(ZoneId.of("Asia/Seoul"));
        String ext = extOf(fileName);
        return String.format("%s/%s/%s/%04d/%02d/%s.%s",
                awsAppProps.getS3().getBasePrefix(), categoryPrefix, awsAppProps.getS3().getEnvPrefix(),
                ym.getYear(), ym.getMonthValue(), UUID.randomUUID(), ext
        );
    }

    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return (i > -1 ? name.substring(i + 1) : "bin").toLowerCase(Locale.ROOT);
    }
}
