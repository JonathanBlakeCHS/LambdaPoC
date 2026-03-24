package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public record ServiceData(
        @JsonProperty String id,
        @JsonProperty String name,
        @JsonProperty String url,
        @JsonProperty String createdOn,
        @JsonProperty String productId,
        @JsonProperty String productName,
        @JsonProperty DataPackageSummary[] versions
) {
    private final static DateTimeFormatter createdOnFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Instant getCreatedOnAsInstant() {
        return Instant.from(createdOnFormat.parse(createdOn));
    }
}
