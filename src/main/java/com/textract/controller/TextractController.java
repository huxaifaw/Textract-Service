package com.textract.controller;

import com.textract.domain.TextractRequest;
import com.textract.service.TextractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/textract")
public class TextractController {

    private final TextractService textractService;

    public TextractController(TextractService textractService) {
        this.textractService = textractService;
    }

    @PostMapping("/extract")
    public ResponseEntity<String> extractText(@RequestBody TextractRequest textractRequest) throws IOException {
        return ResponseEntity.ok(textractService.getDataWithinBoundingBox(textractRequest));
    }
}
