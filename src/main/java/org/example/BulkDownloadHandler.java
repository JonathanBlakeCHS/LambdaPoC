package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.example.service.DownloadService;
import org.example.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.adapter.aws.FunctionInvoker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.SpringHandlerInstantiator;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@SpringBootApplication()
@Configuration
public class BulkDownloadHandler implements RequestHandler<Map<String, String>, String> {
    //    private static final String FILE_NAME = "TEMP_FILE";
    public static final String FILE_NAME = "download.csv";
    public static final String TARGE_BUCKET_NAME = "address-lookup-poc-bulk-storage-public";
    public static final String TARGET_BUCKET_KEY = "key";
    //    private static final String DOWNLOAD_URL = "http://release.ch.gov.uk/chl/feature/beaker_ci/ewf-backend-latest";
    private static final String DOWNLOAD_URL = "http://media0.giphy.com/media/v1.Y2lkPTc5MGI3NjExZHdtZnBnM2hsOTBuYzJzY2IwNjI1NzJudWx6ZTZpZ3oyZXBjbGUwdyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/ehwuBgKNA2NACoFa7w/giphy.gif";
    @Autowired
    DownloadService downloadService;
    @Autowired
    UploadService uploadService;


    public String handleRequest(Map<String, String> event, Context context) {
        System.out.println("Handling request " + event + "  " + context);

        System.out.println("starting download ");
        List<File> downloads = downloadService.downloadCSV();
        System.out.println("download complete");
        downloads.stream().forEach(each -> uploadService.uploadTos3(each));
//            uploadService.uploadTos3(bulkData);
        System.out.println("upload complete");
        return "";
    }
}