package com.textract.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockDTO {
    @JsonProperty("BlockType")
    private String blockType;

    @JsonProperty("Confidence")
    private Float confidence;

    @JsonProperty("Text")
    private String text;

    @JsonProperty("Geometry")
    private GeometryDTO geometry;

    @JsonProperty("Relationships")
    private List<Map<String, Object>> relationships;

    @JsonProperty("Page")
    private Integer page;
}
