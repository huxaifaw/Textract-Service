package com.textract.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.textract.domain.BlockDTO;
import com.textract.domain.BoundingBoxDTO;
import com.textract.domain.TextractRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TextExtractionService {

    private final TextractClient textractClient;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TextExtractionService() {
        this.s3Client = S3Client.create();
        this.textractClient = TextractClient.create();
    }

    public String extractTextFromBoundingBox(TextractRequest textractRequest) throws IOException {
//        String jobId = startTextractAnalysis(textractRequest.getBucket(), textractRequest.getDocumentKey());
//        List<Block> blocks = getTextractResults(jobId, textractRequest.getPageNumber());
//
//        return blocks.stream()
//                .filter(block -> block.blockType() == BlockType.WORD)
//                .filter(block -> isWithinBoundingBox(block.geometry().boundingBox(), textractRequest.getX(), textractRequest.getY(), textractRequest.getWidth(), textractRequest.getHeight()))
//                .map(Block::text)
//                .collect(Collectors.joining(" "));

        List<Block> blocks;
        if (textractRequest.isGetOcrFromS3()) {
            blocks = readTextractBlocks(textractRequest.getBucket(), textractRequest.getOcrDocumentKey());
        } else {
            log.info("Starting OCR generation");
            String jobId = startTextDetection(textractRequest.getBucket(), textractRequest.getDocumentKey());
            blocks = getTextDetectionResults(jobId);
            log.info("OCR generated");
        }

        return blocks.stream()
                .filter(block -> block.blockType() == BlockType.WORD)
                .filter(block -> block.page() == textractRequest.getPageNumber())
                .filter(block -> isWithinBoundingBox(block.geometry().boundingBox(), textractRequest.getX(), textractRequest.getY(), textractRequest.getWidth(), textractRequest.getHeight()))
                .map(Block::text)
                .collect(Collectors.joining(" "));
    }

    private String startTextractAnalysis(String bucket, String documentKey) {
        StartDocumentAnalysisRequest request = StartDocumentAnalysisRequest.builder()
                .documentLocation(doc -> doc.s3Object(obj -> obj.bucket(bucket).name(documentKey)))
                .featureTypes(FeatureType.TABLES, FeatureType.FORMS)
                .build();

        StartDocumentAnalysisResponse response = textractClient.startDocumentAnalysis(request);
        log.info("Started Textract job with Job ID: {}", response.jobId());
        return response.jobId();
    }

    private List<Block> getTextractResults(String jobId, int pageNumber) {
        GetDocumentAnalysisRequest request = GetDocumentAnalysisRequest.builder()
                .jobId(jobId)
                .build();

        while (true) {
            GetDocumentAnalysisResponse response = textractClient.getDocumentAnalysis(request);
            JobStatus status = response.jobStatus();

            if (status == JobStatus.SUCCEEDED) {
                log.info("Textract job completed successfully.");
                return response.blocks().stream()
                        .filter(block -> block.page() == pageNumber)
                        .toList();
            } else if (status == JobStatus.FAILED) {
                throw new RuntimeException("Textract job failed.");
            }

            log.info("Waiting for Textract job to complete...");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for Textract job", e);
            }
        }
    }

    private String startTextDetection(String bucket, String documentKey) {
        StartDocumentTextDetectionRequest request = StartDocumentTextDetectionRequest.builder()
                .documentLocation(doc -> doc.s3Object(obj -> obj.bucket(bucket).name(documentKey)))
                .build();

        StartDocumentTextDetectionResponse response = textractClient.startDocumentTextDetection(request);
        return response.jobId();
    }

    private List<Block> getTextDetectionResults(String jobId) {
        GetDocumentTextDetectionRequest request = GetDocumentTextDetectionRequest.builder()
                .jobId(jobId)
                .build();

        while (true) {
            GetDocumentTextDetectionResponse response = textractClient.getDocumentTextDetection(request);

            if (response.jobStatus() == JobStatus.SUCCEEDED) {
                return response.blocks();
            } else if (response.jobStatus() == JobStatus.FAILED) {
                throw new RuntimeException("Textract job failed.");
            }

            log.info("Waiting for Textract job to complete...");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for Textract job", e);
            }
        }
    }

    private boolean isWithinBoundingBox(BoundingBox box, float x, float y, float width, float height) {
        float bx = box.left();
        float by = box.top();
        float bwidth = box.width();
        float bheight = box.height();

        return bx >= x && bx + bwidth <= x + width &&
                by >= y && by + bheight <= y + height;
    }

    public List<Block> readTextractBlocks(String bucketName, String documentKey) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(documentKey)
                .build();

        try (ResponseInputStream<?> s3Object = s3Client.getObject(getObjectRequest)) {
            List<BlockDTO> blockDTOs = objectMapper.readValue(s3Object, new TypeReference<>() {
            });

            return blockDTOs.stream().map(dto -> Block.builder()
                    .blockType(dto.getBlockType())
                    .page(dto.getPage())
                    .confidence(dto.getConfidence())
                    .geometry(geo -> geo.boundingBox(bb -> {
                        BoundingBoxDTO bbox = dto.getGeometry() != null ? dto.getGeometry().getBoundingBox() : null;
                        if (bbox != null) {
                            bb.width(bbox.getWidth())
                                    .height(bbox.getHeight())
                                    .left(bbox.getLeft())
                                    .top(bbox.getTop());
                        }
                    }))
                    .text(dto.getText())
                    .relationships(new ArrayList<>())
                    .build()
            ).toList();
        }
    }
}
