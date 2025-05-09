package com.textract.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeometryDTO {

    @JsonProperty("BoundingBox")
    private BoundingBoxDTO boundingBox;
}
