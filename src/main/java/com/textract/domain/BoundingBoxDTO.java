package com.textract.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoundingBoxDTO {

    @JsonProperty("Left")
    private Float left;

    @JsonProperty("Top")
    private Float top;

    @JsonProperty("Width")
    private Float width;

    @JsonProperty("Height")
    private Float height;
}
