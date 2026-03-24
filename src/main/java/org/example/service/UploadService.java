package org.example.service;

import static org.example.BulkDownloadHandler.TARGET_BUCKET_KEY;
import static org.example.BulkDownloadHandler.TARGET_BUCKET_NAME;
import org.example.dto.Downloads;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class UploadService {
    public static final int MULTIPART_SIZE = (5 * 1024 * 1024);
    @Autowired
    S3AsyncClient asyncClient;
    @Autowired
    S3Client client;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    DownloadService downloadService;

    /**
     * Asynchronous upload with Transfer manager of file in local memory
     *
     * @param file file to be placed in s3 bucket
     * @return async completion of upload
     */
    public FileUpload uploadTos3(File file) {
        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                .putObjectRequest(b -> b
                        .bucket(TARGET_BUCKET_NAME).key(TARGET_BUCKET_KEY + file.getName())
                )
                .source(file)
                .build();

        try (S3TransferManager transferManager = S3TransferManager.builder()
                .s3Client(asyncClient)
                .build()) {
            return transferManager.uploadFile(uploadFileRequest);
        }
    }

    public PutObjectResponse syncUploadTos3(File temporaryFile) {
        return client.putObject(
                b -> b.bucket(TARGET_BUCKET_NAME).key(TARGET_BUCKET_KEY + temporaryFile.getName()),
                temporaryFile.toPath()
        );
    }


    /**
     * Execute restTemplate and attempt to create input stream from response body to put synchronously
     *
     * @param apiKey os api key
     * @param download metadata of file to be downloaded
     */
    public void transfer_alt(String apiKey, Downloads download) {
        //noinspection ConstantValue
        if (true) {
            throw new RuntimeException("This code will fail and is just included for others to see solutions I've looked at already");
        }
        restTemplate.execute(download.url(), HttpMethod.GET,
                request -> request.getHeaders().set("key", apiKey),
                clientHttpResponse -> {

                    RequestBody requestBody = RequestBody.fromInputStream(
                            clientHttpResponse.getBody(),
                            download.size().longValue()
                    );

                    return client
                            .putObject(
                                    r -> r
                                            .bucket(TARGET_BUCKET_NAME)
                                            .key(TARGET_BUCKET_KEY + download.fileName()),
                                    requestBody
                            );
                });
    }

    /**
     * Execute restTemplate and attempt to create input stream from response body to put asynchronously
     *
     * @param apiKey os api key
     * @param download metadata of file to be downloaded
     */
    public CompletableFuture<PutObjectResponse> transferAsync(String apiKey, Downloads download) {
        //noinspection ConstantValue
        if (true) {
            throw new RuntimeException("This code will fail and is just included for others to see solutions I've looked at already");
        }
        System.out.println("Getting data ");
        return restTemplate.execute(download.url(), HttpMethod.GET,
                request -> request.getHeaders().set("key", apiKey),
                clientHttpResponse -> uploadClientResponse(clientHttpResponse, download)); // Fails because the response body isn't a retry-able stream.
    }

    /**
     * Execute query from URL and attempt to create input stream from response body to put asynchronously
     *
     * @param download metadata of file to be downloaded
     */
    public CompletableFuture<PutObjectResponse> transfer_C(Downloads download) {
        //noinspection ConstantValue
        if (true) {
            throw new RuntimeException("This code will fail and is just included for others to see solutions I've looked at already");
        }
        AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromInputStream(b -> {
                    try {
                        b
                                .inputStream(URI.create(download.url()).toURL().openStream())//Think this is going to fail because of headers
                                .contentLength(download.size().longValue())
                                .executor(Executors.newSingleThreadExecutor());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } // Could swap this to a shared Executor pool so that
        );

        return asyncClient
                .putObject(
                        r -> r
                                .bucket(TARGET_BUCKET_NAME)
                                .key(TARGET_BUCKET_KEY + download.fileName()),
                        asyncRequestBody
                );
    }


    /**
     * Manually create synchronous multipart requests to limit memory space usage at a time
     *
     * @param download metadata of file to be downloaded
     */
    public void transferWithMultipartRequest(Downloads download) {
        String uploadId = "";
        // Upload the parts of the file.
        int partNumber = 1;
        List<CompletedPart> completedParts = new ArrayList<>();
        ByteBuffer bb = ByteBuffer.allocate(MULTIPART_SIZE * 2); // 5 MB byte buffer
        // Rest Template range isn't returning a consistent size, so I doubled the buffer to be sure it doesn't overflow it.
        try {
            CreateMultipartUploadResponse createMultipartUploadResponse = client.createMultipartUpload(
                    builder -> builder
                            .bucket(TARGET_BUCKET_NAME)
                            .key(TARGET_BUCKET_KEY + download.fileName()).build()
            );
            uploadId = createMultipartUploadResponse.uploadId();

            long fileSize = download.size().longValue();

            System.out.println(download.fileName() + "   " + download.url());

            int position = 0;
            while (position < fileSize) {
                System.out.println("putting part " + partNumber);
                byte[] nextPart = downloadService.getNextBytes(download, position, MULTIPART_SIZE);
                System.out.println("retrieved next bytes " + nextPart.length + " " + bb.capacity());
//                bb = ByteBuffer.wrap(nextPart); // Creating a new byte buffer was slower than just doubling the size
//                of the byte buffer with no significant memory difference
                bb.put(nextPart);
                bb.flip(); // Swap position and limit before reading from the buffer.
                CompletedPart part = uploadNextPart(download, uploadId, partNumber, bb);
                completedParts.add(part);

                bb.clear();
                position += MULTIPART_SIZE;
                partNumber++;
            }
            // Complete the multipart upload.
            String finalUploadId = uploadId;
            client.completeMultipartUpload(b -> b
                    .bucket(TARGET_BUCKET_NAME)
                    .key(TARGET_BUCKET_KEY + download.fileName())
                    .uploadId(finalUploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build()));
        } catch (Exception e) {
            String finalUploadId1 = uploadId;
            client.abortMultipartUpload(b -> b
                    .uploadId(finalUploadId1)
                    .bucket(TARGET_BUCKET_NAME).key(TARGET_BUCKET_KEY + download.fileName()));
            throw new RuntimeException(e);
        }
    }

    private CompletedPart uploadNextPart(Downloads download, String uploadId, int partNumber, ByteBuffer bb) {
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(TARGET_BUCKET_NAME)
                .key(TARGET_BUCKET_KEY + download.fileName())
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();

        UploadPartResponse partResponse = client.uploadPart(
                uploadPartRequest,
                RequestBody.fromByteBuffer(bb));

        return CompletedPart.builder()
                .partNumber(partNumber)
                .eTag(partResponse.eTag())
                .build();
    }

    /**
     * Prototype async manual multipart upload that wasn't completed because I didn't know how it would handle concurrency and exceptions
     * Intended to be equivalent to transferWithMultipartRequest
     *
     * @param download metadata of file to be downloaded
     */
    public void transfer_e(Downloads download) {
        AtomicReference<String> uploadId = new AtomicReference<>("");
        CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadResponseFuture = asyncClient.createMultipartUpload(
                builder -> builder
                        .bucket(TARGET_BUCKET_NAME)
                        .key(TARGET_BUCKET_KEY + download.fileName()).build()
        );
        createMultipartUploadResponseFuture.handle(
                        (createMultipartUploadResponse, other) -> {

                            uploadId.set(createMultipartUploadResponse.uploadId());

                            // Upload the parts of the file.
                            int partNumber = 1;
                            List<CompletedPart> completedParts = new ArrayList<>();
                            ByteBuffer bb = ByteBuffer.allocate(MULTIPART_SIZE * 2); // 5 MB byte buffer. See comment on sync version for why this is doubled


                            long fileSize = download.size().longValue();


                            int position = 0;
                            while (position < fileSize) {
                                byte[] nextPart;
                                nextPart = downloadService.getNextBytes(download, position, MULTIPART_SIZE);
                                bb.put(nextPart);
                                bb.flip(); // Swap position and limit before reading from the buffer.
                                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                                        .bucket(TARGET_BUCKET_NAME)
                                        .key(TARGET_BUCKET_KEY + download.fileName())
                                        .uploadId(uploadId.get())
                                        .partNumber(partNumber)
                                        .build();

                                int finalPartNumber = partNumber;
                                CompletedPart partResponse = asyncClient.uploadPart(
                                        uploadPartRequest,
                                        AsyncRequestBody.fromByteBuffer(bb)).handle(
                                        (uploadPartResponse, throwable) -> CompletedPart.builder()
                                                .partNumber(finalPartNumber)
                                                .eTag(uploadPartResponse.eTag())
                                                .build()
                                ).join();
                                completedParts.add(partResponse);

                                bb.clear();
                                position += MULTIPART_SIZE;
                                partNumber++;
                            }
                            return Map.of("UPLOAD_ID", uploadId,
                                    "completedParts", completedParts);
                        }
                ).handle((complete, throwable) -> client.completeMultipartUpload(b -> b
                        .bucket(TARGET_BUCKET_NAME)
                        .key(TARGET_BUCKET_KEY + download.fileName())
                        .uploadId((String) complete.get("UPLOAD_ID"))
                        .multipartUpload(CompletedMultipartUpload.builder().parts((Collection<CompletedPart>) complete.get("completedParts")).build())))
//            .exceptionally(throwable -> AbortMultipartUploadRequest.builder().uploadId(uploadId.get()).bucket(TARGET_BUCKET_NAME).key(TARGET_BUCKET_KEY+download.fileName()).build())
                .join();
    }

    private CompletableFuture<PutObjectResponse> uploadClientResponse(ClientHttpResponse clientHttpResponse, Downloads download) {
        try {
            InputStream body = clientHttpResponse.getBody();

            AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromInputStream(b -> b
                    .inputStream(body)// Must be retry-able
                    .contentLength(clientHttpResponse.getHeaders().getContentLength())
                    .executor(Executors.newSingleThreadExecutor()) // Could swap this to a shared Executor pool to control concurrent attempts
            );

            return asyncClient
                    .putObject(
                            r -> r
                                    .bucket(TARGET_BUCKET_NAME)
                                    .key(TARGET_BUCKET_KEY + download.fileName()),
                            asyncRequestBody
                    );
        } catch (IOException | RetryableException e) {
            System.out.println("Upload failed for " + download.fileName());
            e.printStackTrace();
        }
        return new CompletableFuture<>();
    }
}
