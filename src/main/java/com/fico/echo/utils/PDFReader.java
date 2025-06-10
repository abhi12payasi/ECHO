package com.fico.echo.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;

public class PDFReader {
    public static String extractText(String path) throws IOException {
        try (PDDocument document = PDDocument.load(new File(path))) {
            return new PDFTextStripper().getText(document);
        }
    }
}
