package org.example.service;

import static org.example.BulkDownloadHandler.TARGET_BUCKET_KEY;
import static org.example.BulkDownloadHandler.TARGE_BUCKET_NAME;
import org.example.dto.Downloads;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UploadService {
    @Autowired
    S3AsyncClient client;
    @Autowired
    RestTemplate restTemplate;

    public void uploadTos3(File temporaryFile) {
        client.putObject(
                b -> b.bucket(TARGE_BUCKET_NAME).key(TARGET_BUCKET_KEY).build(),
                temporaryFile.toPath()
        );


        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                .putObjectRequest(b -> b
                        .bucket(TARGE_BUCKET_NAME)
                )
//                .addTransferListener(LoggingTransferListener.create())  // Add listener.
                .source(temporaryFile)
                .build();

        FileUpload fileUpload;
        try (S3TransferManager transferManager = S3TransferManager.builder()
                .s3Client(client)
                .build()) {
            fileUpload = transferManager.uploadFile(uploadFileRequest);
        }

        fileUpload.completionFuture().join();
    }

    public void transfer(String apiKey, Downloads download) {

        System.out.println("Getting data ");
        restTemplate.execute(download.url(), HttpMethod.GET,
                request -> {
                    request.getHeaders().set("key", apiKey);
                },
                clientHttpResponse -> {
                    File tempFile = File.createTempFile("", "");
                    File file = new File("This Is Stupid") {

                    };
                    UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                            .putObjectRequest(b -> b
                                    .bucket(TARGE_BUCKET_NAME)
                            )
//                .addTransferListener(LoggingTransferListener.create())  // Add listener.
                            .source(file)
                            .build();

                    client.putObject(
                            b -> b.bucket(TARGE_BUCKET_NAME).key(TARGET_BUCKET_KEY).build(),
                            Path.of("test")
                    );

                    return null;
                });
    }
}
