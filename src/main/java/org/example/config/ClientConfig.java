package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class ClientConfig {
    @Bean
    S3AsyncClient asyncClient(AwsCredentialsProvider credentialsProvider) {
        return S3AsyncClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.EU_WEST_2).build();
    }

    @Bean
    S3Client client(AwsCredentialsProvider credentialsProvider) {
        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.EU_WEST_2)
                .build();
    }

    @Bean
    AwsCredentialsProvider credentialsProvider() {
        return DefaultCredentialsProvider.builder().build();
    }
}
