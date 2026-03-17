package org.example.config;

import com.amazonaws.services.lambda.runtime.Context;
import org.example.BulkDownloadHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Configuration
public class InvokerConfig {
    @Autowired
    BulkDownloadHandler bulkDownloadHandler;

    @Bean
    public BiFunction<Map<String, String>, Context, String> bulkFunction() {
        return (event, context)-> bulkDownloadHandler.handleRequest(event, context);
    }
}
