package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.dto.enums.DataFormat;
import org.example.dto.enums.ReasonType;
import org.example.dto.enums.SupplyType;

public record DataPackage(
        @JsonProperty String id,
        @JsonProperty String url,
        @JsonProperty String createdOn,
        @JsonProperty ReasonType reason,
        @JsonProperty SupplyType supplyType,
        @JsonProperty String productVersion,
        @JsonProperty DataFormat format,
        @JsonProperty String dataPackageUrl,
        @JsonProperty String nextVersionUrl,
        @JsonProperty Downloads[] downloads
) {
}
