package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;

@SpringBootApplication()
@ComponentScan({"org.example"})
public class LocalHarness implements CommandLineRunner {
    @Autowired
    BulkDownloadHandler bulkDownloadHandler;

    public static void main(String[] args) {
//        int exitCode = new CommandLine(new LocalHarness()).execute(args);
//        System.exit(exitCode);
        System.out.println("Starting");
//new String[]{"debug=true"}
        SpringApplication.run(LocalHarness.class, args);
        System.out.println("Finished");
    }

    @Override
    public void run(String... args) throws Exception {
        bulkDownloadHandler.handleRequest(Map.of(), null);
    }
}
