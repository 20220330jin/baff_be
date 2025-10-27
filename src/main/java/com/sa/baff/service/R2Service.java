package com.sa.baff.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class R2Service {

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicBaseUrl;

    public R2Service(S3Client s3Client,
                     @Value("${cloud.r2.bucket-name}") String bucketName,
                     @Value("${cloud.r2.public-url}") String publicUrl) {  // 이 부분 변경!
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        // Public Development URL 사용
        this.publicBaseUrl = publicUrl;
    }

    /**
     * 다중 파일을 R2에 업로드하고 저장된 URL 리스트를 반환합니다.
     */
    public List<String> uploadFiles(List<MultipartFile> files) {
        return files.stream()
                .map(this::uploadSingleFile)
                .toList();
    }

    /**
     * 단일 파일을 R2에 업로드하고 공개 URL을 반환합니다.
     */
    public String uploadSingleFile(MultipartFile file) {
        if (file.isEmpty()) {
            return null;
        }

        // 고유한 파일 키(이름) 생성: UUID로 충돌 방지
        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains(".")) ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        // review-images/ 폴더 안에 UUID.확장자 형식으로 저장
        String key = "review-images/" + UUID.randomUUID() + extension;

        try {
            // R2 업로드 요청 빌드 및 실행
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 최종 공개 URL 반환
            // 결과: https://pub-78a207e919384d84b1793bae2debbc56.r2.dev/review-images/abc-123.png
            return publicBaseUrl + "/" + key;

        } catch (IOException e) {
            throw new RuntimeException("R2 파일 업로드 실패: " + e.getMessage(), e);
        }
    }
}