package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.dto.enums.DataFormat;
import org.example.dto.enums.ReasonType;
import org.example.dto.enums.SupplyType;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public record DataPackageSummary(
        @JsonProperty String id,
        @JsonProperty String url,
        @JsonProperty String createdOn,
        @JsonProperty ReasonType reason,
        @JsonProperty SupplyType supplyType,
        @JsonProperty String productVersion,
        @JsonProperty DataFormat format
) {
    private final static DateTimeFormatter createdOnFormat = DateTimeFormatter.ofPattern("yyyy-mm-dd");

    public Instant getCreatedOnAsInstant() {
        return Instant.from(createdOnFormat.parse(createdOn));
    }
}
