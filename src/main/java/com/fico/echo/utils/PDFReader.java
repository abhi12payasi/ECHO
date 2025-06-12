package com.fico.echo.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PDFReader {
    public static String extractText(String path) throws IOException {
        try (PDDocument document = PDDocument.load(new File(path))) {
            return new PDFTextStripper().getText(document);
        }
    }

//    public static void loadAllPdfChunks(List<String> pdfChunks) throws IOException {
//        File folder = new ClassPathResource("docs").getFile();
//        File[] pdfFiles = folder.listFiles((dir, name) -> name.endsWith(".pdf"));
//        assert pdfFiles != null;
//        for (File pdfFile : pdfFiles) {
//            pdfChunks.addAll(extractChunksFromPdf(pdfFile, 1000));
//        }
//    }

    public static void loadAllPdfChunks(List<String> pdfChunks) throws IOException {
        File docsFolder = new ClassPathResource("docs").getFile();
        Path docsPath = docsFolder.toPath();

        try (Stream<Path> paths = Files.walk(docsPath)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .forEach(path -> {
                        try {
                            pdfChunks.addAll(extractChunksFromPdf(path.toFile(), 1000));
                        } catch (IOException e) {
                            // You can log or handle individual file errors here
                            e.printStackTrace();
                        }
                    });
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
