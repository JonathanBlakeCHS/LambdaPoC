package org.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;

@SpringBootApplication()
@ComponentScan({"org.example"})
public class LocalHarness implements CommandLineRunner {
    public static void main(String[] args) {
        System.out.println("Starting");
        SpringApplication.run(LocalHarness.class, args);
        System.out.println("Finished");
    }

    @Override
    public void run(String... args) {
        new BulkDownloadHandler().handleRequest(Map.of(), null);
    }
}
