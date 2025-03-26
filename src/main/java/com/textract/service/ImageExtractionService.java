package com.textract.service;

import com.textract.domain.TextractRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Component
public class ImageExtractionService {
    private final S3Client s3Client;

    public ImageExtractionService() {
        this.s3Client = S3Client.create();
    }

    public String getImageWithinBoundingBox(TextractRequest textractRequest) throws IOException {
        byte[] pdfBytes = fetchPdfFromS3(textractRequest.getBucket(), textractRequest.getDocumentKey());
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFRenderer renderer = new PDFRenderer(document);

            // Convert PDF page to an image (increased DPI for better quality)
            BufferedImage fullImage = renderer.renderImage(textractRequest.getPageNumber() - 1, 2.0f);

            // Convert normalized bounding box to pixel values
            int imageWidth = fullImage.getWidth();
            int imageHeight = fullImage.getHeight();

            int cropX = (int) (textractRequest.getX() * imageWidth);
            int cropY = (int) (textractRequest.getY() * imageHeight);
            int cropWidth = (int) (textractRequest.getWidth() * imageWidth);
            int cropHeight = (int) (textractRequest.getHeight() * imageHeight);

            // Ensure values stay within bounds
            cropX = Math.max(0, Math.min(cropX, imageWidth - 1));
            cropY = Math.max(0, Math.min(cropY, imageHeight - 1));
            cropWidth = Math.max(1, Math.min(cropWidth, imageWidth - cropX));
            cropHeight = Math.max(1, Math.min(cropHeight, imageHeight - cropY));
            BufferedImage croppedImage = fullImage.getSubimage(cropX, cropY, cropWidth, cropHeight);

            return encodeImageToBase64(croppedImage);
        }
    }

    private byte[] fetchPdfFromS3(String bucketName, String documentKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(documentKey)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asByteArray();
    }

    private String encodeImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}
