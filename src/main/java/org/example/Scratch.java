package org.example;

import static org.example.service.DownloadService.DATA_PACKAGE_ID;
import static org.example.service.DownloadService.OS_API_KEY;
import org.example.config.HTTPConfig;
import org.example.dto.DataPackage;
import org.example.dto.Downloads;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"CommentedOutCode", "UnusedAssignment"})
public class Scratch {
    public static void main(String[] args) throws IOException {

        String urlString = "https://api.os.uk/downloads/v1/dataPackages/" + DATA_PACKAGE_ID;
//        urlString = "https://api.os.uk/downloads/v1/dataPackages/18708/versions/120117";
        urlString = "https://api.os.uk/downloads/v1/dataPackages/18708/versions/124538";


        HttpHeaders headers = new HttpHeaders();
        headers.set("key", OS_API_KEY);
        HttpEntity<Object> entity = new HttpEntity<>(headers);

//        ResponseEntity<ServiceData> serviceData = new HTTPConfig().getRestTemplate()
//                .exchange(urlString, HttpMethod.GET, entity, ServiceData.class);
        ResponseEntity<DataPackage> serviceData = new HTTPConfig().getRestTemplate()
                .exchange(urlString, HttpMethod.GET, entity, DataPackage.class);
        System.out.println(serviceData);
//        for (DataPackageSummary version : serviceData.getBody().versions()) {
//            System.out.println(version);
//        }
        DataPackage serviceDataBody = serviceData.getBody();
        for (Downloads version : Objects.requireNonNull(serviceDataBody).downloads()) {
            System.out.println(version);
        }
        Downloads download = serviceDataBody.downloads()[0];
        System.out.println(Arrays.toString(getNextBytes(download, 0, 100)));
    }

    /**
     * Throws a 400 error for some reason
     * Local testing function to try and figure out behaviour of getNextBytes for the upload service
     * @param download metadata surround file to be partitioned and downloaded
     * @param position starting position
     * @param multipartSize size of partition to be downloaded
     * @return list of bytes
     * @throws IOException
     */
    public static byte[] getNextBytes(Downloads download, int position, int multipartSize) throws IOException {
        URL url = URI.create(download.url()).toURL();
        System.out.println(download.url() + "   " + url);
        HttpURLConnection httpConnection;
        httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.setRequestMethod("GET");
        httpConnection.setRequestProperty("key", OS_API_KEY);
        httpConnection.setRequestProperty(
                "Range",
                "bytes=" + position + "-" + Math.min(download.size(), position + multipartSize)
        );

        return httpConnection.getInputStream().readAllBytes();
    }

    /**
     * Throws a 400 error for some reason
     * Local testing function to try and figure out behaviour of getNextBytes for the upload service
     * @param download metadata surround file to be partitioned and downloaded
     * @param position starting position
     * @param multipartSize size of partition to be downloaded
     * @return list of bytes
     * @throws IOException
     */
    public static byte[] getNextBytesRestTemplate(Downloads download, int position, int multipartSize) {
        RestTemplate restTemplate = new HTTPConfig().getRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("key", OS_API_KEY);
        headers.setRange(List.of(HttpRange.createByteRange(position, position + multipartSize)));
        HttpEntity<Object> entity = new HttpEntity<>(headers);

        String test = restTemplate.exchange(download.url(), HttpMethod.GET, entity, String.class).getBody();
        System.out.println(test);
        assert test != null;
        return test.getBytes();
    }
}
