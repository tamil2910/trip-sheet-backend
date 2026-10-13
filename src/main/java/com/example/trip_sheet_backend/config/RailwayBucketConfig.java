package com.example.trip_sheet_backend.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
public class RailwayBucketConfig {
  @Bean(destroyMethod = "close")
  @ConditionalOnExpression("'${BUCKET_ENDPOINT:}'.length() > 0 && '${BUCKET_ACCESS_KEY_ID:}'.length() > 0 && '${BUCKET_SECRET_ACCESS_KEY:}'.length() > 0")
  S3Client railwayS3Client(
      @Value("${BUCKET_ENDPOINT:}") String endpoint,
      @Value("${BUCKET_ACCESS_KEY_ID:}") String accessKey,
      @Value("${BUCKET_SECRET_ACCESS_KEY:}") String secretKey,
      @Value("${BUCKET_REGION:auto}") String region,
      @Value("${BUCKET_PATH_STYLE:false}") boolean pathStyle) {
    if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
      return null;
    }
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(StringUtils.hasText(region) ? region : "auto"))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build())
        .build();
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnExpression("'${BUCKET_ENDPOINT:}'.length() > 0 && '${BUCKET_ACCESS_KEY_ID:}'.length() > 0 && '${BUCKET_SECRET_ACCESS_KEY:}'.length() > 0")
  S3Presigner railwayS3Presigner(
      @Value("${BUCKET_ENDPOINT:}") String endpoint,
      @Value("${BUCKET_ACCESS_KEY_ID:}") String accessKey,
      @Value("${BUCKET_SECRET_ACCESS_KEY:}") String secretKey,
      @Value("${BUCKET_REGION:auto}") String region,
      @Value("${BUCKET_PATH_STYLE:false}") boolean pathStyle) {
    if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
      return null;
    }
    return S3Presigner.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(StringUtils.hasText(region) ? region : "auto"))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build())
        .build();
  }
}
