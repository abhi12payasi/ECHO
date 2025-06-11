package com.fico.echo.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFReader {
    public static String extractText(String path) throws IOException {
        try (PDDocument document = PDDocument.load(new File(path))) {
            return new PDFTextStripper().getText(document);
        }
    }
    public static void loadAllPdfChunks(List<String> pdfChunks) throws IOException {
        File folder = new ClassPathResource("docs").getFile();
        File[] pdfFiles = folder.listFiles((dir, name) -> name.endsWith(".pdf"));
        assert pdfFiles != null;
        for (File pdfFile : pdfFiles) {
            pdfChunks.addAll(extractChunksFromPdf(pdfFile, 1000));
        }
    }
    private static List<String> extractChunksFromPdf(File file, int chunkSize) throws IOException {
        PDDocument document = PDDocument.load(file);
        PDFTextStripper stripper = new PDFTextStripper();
        String fullText = stripper.getText(document);
        document.close();

        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < fullText.length(); i += chunkSize) {
            int end = Math.min(fullText.length(), i + chunkSize);
            chunks.add(fullText.substring(i, end));
        }
        return chunks;
    }
}
