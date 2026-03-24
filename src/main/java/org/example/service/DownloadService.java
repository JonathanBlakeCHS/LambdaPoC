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
import org.springframework.http.HttpRange;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DownloadService {
    public static final String OS_API_KEY = "qGTNATFjX2iHpbLi3L1oFs7tGM4AlwEv";
    public static final String DATA_PACKAGE_ID = "18708";
    @Autowired
    RestTemplate restTemplate;

    // This doesn't match the real data structure and needs to be changed if we want to try it.
    // It was just made to test options and consistency
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

    private static Optional<DataPackageSummary> getMostRecentDataInitialisation(ServiceData serviceDataBody) {
        return Arrays.stream(Objects.requireNonNull(serviceDataBody).versions())
                .filter(dataPackageSummary -> dataPackageSummary.supplyType() == SupplyType.FULL)
                .max(Comparator.comparing(DataPackageSummary::getCreatedOnAsInstant));
    }

    private static HttpEntity<Object> getEntityWithHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("key", OS_API_KEY);
        return new HttpEntity<>(headers);
    }

    public List<File> downloadCSV() {
        System.out.println("In Download Service");
        String urlString = "https://api.os.uk/downloads/v1/dataPackages/" + DATA_PACKAGE_ID;
//        URL url = java.net.URI.create(urlString).toURL();

        return downloadWithRestTemplate();
//        canItBeThisEasy(url);///???
//        downloadWithFileChannels(url, false);
//        return Paths.get(FILE_NAME).toFile();
    }

    // This doesn't match the real data structure and needs to be changed if we want to try it.
    // It was just made to test options and consistency and how to set headings
    private void rawURLFunctions(URL url) throws IOException {
        InputStream in = url.openStream();
        Files.copy(in, Paths.get("tmp/" + FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Download Complete");
    }

    /**
     * Uses rest template to synchronously download files and returns them as a list
     *
     * @return List of downloaded files
     */
    private List<File> downloadWithRestTemplate() {
        return getDownloads().stream()
                .filter(downloads -> downloads.fileName().contains(".json"))
                .map(download -> {
                    System.out.println("Fetching file " + download);
                    return saveToFile(download);
                }).toList();
    }

    public File saveToFile(Downloads initialUpdate) {
        System.out.println("Getting data ");
        File ret = Paths.get("/tmp/" + initialUpdate.fileName()).toFile();
        if (!ret.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                ret.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return restTemplate.execute(initialUpdate.url(), HttpMethod.GET,
                request -> request.getHeaders().set("key", OS_API_KEY),
                clientHttpResponse -> {
                    StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
                    System.out.println("Copied data to " + ret.getAbsolutePath());
                    return ret;
                });
    }

    /**
     * Function used to find the most recent data initialization
     * and then return the download information of the associated files
     *
     * @return List of Download metadata for the most recent initalisation package
     */
    public List<Downloads> getDownloads() {
        System.out.println("Constructing rest template Query");
        HttpEntity<Object> entity = getEntityWithHeaders();

        ServiceData serviceDataBody = getServiceData(entity);
        Optional<DataPackageSummary> initialSetup = getMostRecentDataInitialisation(serviceDataBody);
        if (initialSetup.isPresent()) {
            DataPackage downloadInformation = getDataPackage(initialSetup.get(), entity);
            return Arrays.asList(downloadInformation.downloads());
        } else {
            return List.of();
        }
    }

    private ServiceData getServiceData(HttpEntity<Object> entity) {
        ResponseEntity<ServiceData> serviceData = restTemplate
                .exchange("https://api.os.uk/downloads/v1/dataPackages/" + DATA_PACKAGE_ID, HttpMethod.GET, entity, ServiceData.class);
        System.out.println("Executed rest template Query");

        return serviceData.getBody();
    }

    private DataPackage getDataPackage(DataPackageSummary initial, HttpEntity<Object> entity) {
        System.out.println("Data initialisation found at: " + initial);
        ResponseEntity<DataPackage> downloadInformation = restTemplate
                .exchange(initial.url(), HttpMethod.GET, entity, DataPackage.class);
        return downloadInformation.getBody();
    }

    public byte[] getNextBytes(Downloads download, int position, int multipartSize) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("key", OS_API_KEY);
        headers.setRange(List.of(HttpRange.createByteRange(position, position + (multipartSize / 8))));
        HttpEntity<Object> entity = new HttpEntity<>(headers);

        return Objects.requireNonNull(restTemplate.exchange(download.url(), HttpMethod.GET, entity, String.class).getBody()).getBytes();
    }

}
