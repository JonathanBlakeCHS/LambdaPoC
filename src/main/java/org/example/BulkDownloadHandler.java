package org.example;

import static org.example.service.DownloadService.OS_API_KEY;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.example.service.DownloadService;
import org.example.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

//@Service
//@SpringBootApplication()
//@Configuration
public class BulkDownloadHandler implements RequestHandler<Map<String, String>, String> {
    public static final String FILE_NAME = "download.csv";
    public static final String TARGET_BUCKET_NAME = "address-lookup-poc-bulk-storage-public";
    public static final String TARGET_BUCKET_KEY = "key";
    @Autowired
    DownloadService downloadService;
    @Autowired
    UploadService uploadService;

    public BulkDownloadHandler() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                BulkDownloadHandler.class.getPackage().getName());
        ctx.getAutowireCapableBeanFactory().autowireBean(this);
    }

    public static void main(String[] args) {
        BulkDownloadHandler bdh = new BulkDownloadHandler();
    }

    @SuppressWarnings("DataFlowIssue")// Using switch to enable and disable different bits of logic
    public String handleRequest(Map<String, String> event, Context context) {
        System.out.println("Handling request " + event + "  " + context);
        int toggle = 3;
        switch (toggle){
            case 0:{
                System.out.println("starting download ");
                List<File> downloads = downloadService.downloadCSV();
                System.out.println("download complete");
                downloads.stream().map(each -> uploadService.uploadTos3(each))
                        .toList().forEach(each -> each.completionFuture().join());
                // Using to List to make sure they are all started before waiting.
            }
            case 1:{
                //Testing different way of handling sync orders
                downloadService.getDownloads()
                        .stream()
                        .map(downloadService::saveToFile)
                        .forEach(each -> {
        //                    uploadService.syncUploadTos3(each);//
                            uploadService.uploadTos3(each).completionFuture().join();//clumsily upload synchronously to minimise tmp space used
                            each.delete();
                        });
            }
            case 2:{
                downloadAndUploadCSV();
            }
            case 3: {
                downloadAndUploadCSV_withManualMultipart();
            }
        }
        System.out.println("upload complete");
        return "";
    }

    public void downloadAndUploadCSV() {
        downloadService.getDownloads().stream()
                .filter(downloads -> downloads.fileName().contains(".json"))//optional used to speed up testing not expected logic
                .map(download -> {
                    System.out.println("Fetching file " + download);
                    return uploadService.transferAsync(OS_API_KEY, download);
                })
                .toList()// Used to attempt them to all be started before waiting for completion.
                .forEach(CompletableFuture::join);
    }

    /**
     *
     */
    public void downloadAndUploadCSV_withManualMultipart() {
        downloadService.getDownloads().stream().filter(downloads -> downloads.fileName().contains(".json")).forEach(downloads -> {
            System.out.println("Fetching file " + downloads);
            uploadService.transferWithMultipartRequest(downloads);//  Hand baked multipart
        });
    }

}