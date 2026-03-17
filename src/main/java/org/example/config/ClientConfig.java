package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Configuration
public class ClientConfig {
    @Bean
    S3AsyncClient client(AwsCredentialsProvider credentialsProvider) {
        return S3AsyncClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.EU_WEST_2).build();
    }

    @Bean
    AwsCredentialsProvider credentialsProvider() {
        return DefaultCredentialsProvider.builder().build();
//                    InstanceProfileCredentialsProvider.builder()
//                            .profileFile(
//                                    () -> {
//                                        try {
//                                            return ProfileFile.builder()
////                                                    .type(ProfileFile.Type.CONFIGURATION)
////                                                    .content(new FileInputStream(
////                                                            Paths.get("/Users/jblake4/.aws/config-backup").toFile()
////                                                    ))
////                                                    .content(new FileInputStream(Paths.get("/Users/jblake4/.aws/sso/cache/675cf4e49047f3f9788f02ef56f47ba9e7c44aa2.json").toFile()))
////                                                    .type(ProfileFile.Type.CREDENTIALS)
//                                                    .build();
//                                        } catch (FileNotFoundException e) {
//                                            throw new RuntimeException(e);
//                                        }
//                                    }
//            )
//                    (AwsCredentialsProvider) StaticCredentialsProvider.create((AwsCredentials) new BasicAWSCredentials("Accesskey", "Secretkey"))
//                    AnonymousCredentialsProvider.create()
//                    DefaultCredentialsProvider.builder().profileFile(
//                                    () -> {
//                                        try {
//                                            return ProfileFile.builder()
//                                                    .type(ProfileFile.Type.CONFIGURATION)
//                                                    .content(new FileInputStream(
//                                                            Paths.get("/Users/jblake4/.aws/config-backup").toFile()
//                                                    ))
//                                                    .build();
//                                        } catch (FileNotFoundException e) {
//                                            throw new RuntimeException(e);
//                                        }
//                                    }).build()
//                            .build()
//                    AssumeRoleRequest.builder()
//                    .roleArn("arn:aws:iam::123456789012:role/my-role")
//                    .roleSessionName("custom-session")
//                    .build()
    }

}
