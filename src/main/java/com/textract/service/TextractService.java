package com.textract.service;

import com.textract.domain.TextractRequest;
import com.textract.util.Constants;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TextractService {
    private final TextExtractionService textExtractionService;
    private final ImageExtractionService imageExtractionService;

    public TextractService(TextExtractionService textExtractionService, ImageExtractionService imageExtractionService) {
        this.textExtractionService = textExtractionService;
        this.imageExtractionService = imageExtractionService;
    }

    public String getDataWithinBoundingBox(TextractRequest textractRequest) throws IOException {
        String data = "";
        if (Constants.TYPE_TEXT.equalsIgnoreCase(textractRequest.getType())) {
            data = textExtractionService.extractTextFromBoundingBox(textractRequest);
        } else if (Constants.TYPE_IMAGE.equalsIgnoreCase(textractRequest.getType())) {
            data = imageExtractionService.getImageWithinBoundingBox(textractRequest);
        }
        return data;
    }
}
