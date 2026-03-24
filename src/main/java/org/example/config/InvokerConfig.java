package org.example.config;

import org.example.BulkDownloadHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.function.Function;

public class InvokerConfig {
    @Autowired
    BulkDownloadHandler bulkDownloadHandler;

    @Autowired
    Function<Map<String, String>, String> functionHandler() {
        return event -> bulkDownloadHandler.handleRequest(event, null);
    }

//    @Bean // Can this work somehow?
//    public BiFunction<Map<String, String>, Context, String> bulkFunction() {
//        return (event, context)-> bulkDownloadHandler.handleRequest(event, context);
//    }
}
