package com.textract.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TextractRequest {
    private String bucket;
    private String documentKey;
    private String ocrDocumentKey;
    private boolean getOcrFromS3 = true;
    private int pageNumber;
    private float x;
    private float y;
    private float width;
    private float height;
    private String type;
}
