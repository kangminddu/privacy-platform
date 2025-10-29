package com.privacy.privacyplatform.storage.service;

import com.privacy.privacyplatform.storage.dto.PresignedUploadUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3Service(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    /**
     * Pre-signed Upload URL 생성 (클라이언트가 S3에 직접 업로드)
     */
    public PresignedUploadUrl generatePresignedUploadUrl(String filename, String contentType) {
        // S3 키 생성: original/uuid_filename.mp4
        String s3Key = "original/" + UUID.randomUUID() + "_" + filename;

        // PutObjectRequest 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        // Pre-signed URL 생성 (10분 유효)
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        log.info("Pre-signed Upload URL 생성: {}", s3Key);

        return PresignedUploadUrl.builder()
                .url(presignedRequest.url().toString())
                .s3Key(s3Key)
                .build();
    }

    /**
     * Pre-signed Download URL 생성 (클라이언트가 S3에서 직접 다운로드)
     */
    public String generatePresignedDownloadUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        // Pre-signed URL 생성 (10분 유효)
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        log.info("Pre-signed Download URL 생성: {}", s3Key);

        return presignedRequest.url().toString();
    }

    /**
     * S3 파일 존재 여부 확인
     */
    public boolean fileExists(String s3Key) {
        try {
            s3Client.headObject(builder -> builder
                    .bucket(bucketName)
                    .key(s3Key));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * S3 파일 삭제
     */
    public void deleteFile(String s3Key) {
        try {
            s3Client.deleteObject(builder -> builder
                    .bucket(bucketName)
                    .key(s3Key));
            log.info("S3 파일 삭제: {}", s3Key);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", s3Key, e);
        }
    }
}