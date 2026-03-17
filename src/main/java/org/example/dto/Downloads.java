package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Downloads(
        @JsonProperty String fileName,
        @JsonProperty String url,
        @JsonProperty Double size,
        @JsonProperty String md5
) {
}
