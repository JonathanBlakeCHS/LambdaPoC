package org.example.service;

import static org.example.BulkDownloadHandler.FILE_NAME;
import org.example.dto.DataPackage;
import org.example.dto.DataPackageSummary;
import org.example.dto.Downloads;
import org.example.dto.ServiceData;
import org.example.dto.enums.SupplyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DownloadService {
    @Autowired
    S3AsyncClient client;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    UploadService uploadService;

    // This doesn't match the real data structure and needs to be changed if we want to try it
    // it was just made to test options and consistency
    private static void downloadWithFileChannels(URL url, boolean overwriteExistingDownload) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        File outputFile = Paths.get(FILE_NAME).toFile();
        if (!overwriteExistingDownload) {
            httpConnection.setRequestMethod("HEAD");
            long removeFileSize = httpConnection.getContentLengthLong();
            long existingFileSize = outputFile.length();
            if (existingFileSize < removeFileSize) {
                httpConnection.setRequestProperty(
                        "Range",
                        "bytes=" + existingFileSize + "-" + removeFileSize
                );
            }
        }
        ReadableByteChannel readableByteChannel = Channels.newChannel(httpConnection.getInputStream());

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            FileChannel fileChannel = fileOutputStream.getChannel();
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }

    public List<File> downloadCSV() {
//        URL url = new URL(DOWNLOAD_URL);


        System.out.println("In Download Service");
        String apiKey = "qGTNATFjX2iHpbLi3L1oFs7tGM4AlwEv";
        String dataPackageId = "18708";
        String urlString = "https://api.os.uk/downloads/v1/dataPackages/" + dataPackageId;
//        URL url = java.net.URI.create(urlString).toURL();

        return downloadWithRestTemplate(apiKey, urlString);


//        canItBeThisEasy(url);///???

//        downloadWithFileChannels(url, false);


//        return Paths.get(FILE_NAME).toFile();
    }

    // This doesn't match the real data structure and needs to be changed if we want to try it
    // it was just made to test options and consistency
    private void canItBeThisEasy(URL url) throws IOException {

//        HttpURLConnection con = (HttpURLConnection) url.openConnection();
//        con.setRequestMethod("GET");
//
//        return mapper.readValue(
//                con.getInputStream(),
//                String.class
//        );


        InputStream in = url.openStream();
        Files.copy(in, Paths.get(FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Download Complete");
    }

    private ArrayList<File> downloadWithRestTemplate(String apiKey, String urlString) {
        System.out.println("Constructing rest template Query");
        HttpHeaders headers = new HttpHeaders();
        headers.set("key", apiKey);
        HttpEntity<Object> entity = new HttpEntity<>(headers);
        ArrayList<File> downloadedFiles = new ArrayList<>();

        ResponseEntity<ServiceData> serviceData = restTemplate
                .exchange(urlString, HttpMethod.GET, entity, ServiceData.class);
        System.out.println("Executed rest template Query");

        Optional<DataPackageSummary> initialSetup = Arrays.stream(Objects.requireNonNull(serviceData.getBody()).versions())
//                .peek(each -> System.out.println(each))
                .filter(dataPackageSummary -> dataPackageSummary.supplyType() == SupplyType.FULL)
                .max(Comparator.comparing(DataPackageSummary::getCreatedOnAsInstant));
        if (initialSetup.isPresent()) {
            DataPackageSummary initial = initialSetup.get();
            System.out.println("Data initialisation found at: " + initial);
            ResponseEntity<DataPackage> downloadInformation = restTemplate
                    .exchange(initial.url(), HttpMethod.GET, entity, DataPackage.class);
            System.out.println(downloadInformation.getBody());
            Arrays.stream(Objects.requireNonNull(downloadInformation.getBody()).downloads())
//                    .parallel()
                    .filter(downloads -> downloads.fileName().contains(".json"))
                    .forEach(download -> {
                        System.out.println("Fetching file " + download);
//                        uploadService.transfer(apiKey, download);
                        File f = saveToFile(apiKey, download);
                        downloadedFiles.add(f);
                    });
        } else {
            System.out.println("Couldn't find initial data load");
        }
        return downloadedFiles;
    }

    private File saveToFile(String apiKey, Downloads initialUpdate) {
        System.out.println("Getting data ");
        File ret = Paths.get( "/tmp/"+initialUpdate.fileName()).toFile();
        if(!ret.exists()) {
            try {
                ret.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return restTemplate.execute(initialUpdate.url(), HttpMethod.GET,
                request -> {
                    request.getHeaders().set("key", apiKey);
                },
                clientHttpResponse -> {
                    StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
                    System.out.println("Copied data to " + ret.getAbsolutePath());
                    return ret;
                });
    }
}
