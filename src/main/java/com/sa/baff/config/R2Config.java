package com.sa.baff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class R2Config {
    @Value("${cloud.r2.access-key-id}") private String accessKeyId;
    @Value("${cloud.r2.secret-access-key}") private String secretAccessKey;
    @Value("${cloud.r2.endpoint}") private String endpoint;
    @Value("${cloud.r2.region}") private String region;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        // S3Client(R2 클라이언트) 생성 및 설정
        return S3Client.builder()
                .httpClient(UrlConnectionHttpClient.builder().build())
                .endpointOverride(URI.create(endpoint)) // R2 엔드포인트 지정
                .region(Region.of(region)) // 'auto' 리전 지정
                .credentialsProvider(StaticCredentialsProvider.create(credentials)) // 인증 정보 제공
                .build();
    }
}
