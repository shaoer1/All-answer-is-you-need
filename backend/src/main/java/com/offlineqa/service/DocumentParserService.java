package com.offlineqa.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class DocumentParserService {

    public String parse(MultipartFile file) {
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (fileName.endsWith(".pdf")) {
                return parsePdf(file);
            }
            if (fileName.endsWith(".docx")) {
                return parseDocx(file);
            }
            if (fileName.endsWith(".xlsx")) {
                return parseXlsx(file);
            }
            if (fileName.endsWith(".csv")) {
                return parseCsv(file);
            }
            return parseText(file);
        } catch (IOException e) {
            throw new IllegalStateException("文档解析失败: " + e.getMessage(), e);
        }
    }

    private String parsePdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String parseDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            return paragraphs.stream().map(XWPFParagraph::getText).collect(Collectors.joining("\n"));
        }
    }

    private String parseXlsx(MultipartFile file) throws IOException {
        DataFormatter formatter = new DataFormatter();
        List<String> lines = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (Sheet sheet : workbook) {
                Iterator<Row> iterator = sheet.rowIterator();
                if (!iterator.hasNext()) {
                    continue;
                }
                Row headerRow = iterator.next();
                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(formatter.formatCellValue(cell).trim());
                }
                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    List<String> parts = new ArrayList<>();
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = row.getCell(i);
                        String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
                        if (!value.isEmpty()) {
                            String header = headers.get(i).isEmpty() ? ("col" + (i + 1)) : headers.get(i);
                            parts.add(header + ": " + value);
                        }
                    }
                    if (!parts.isEmpty()) {
                        lines.add("[" + sheet.getSheetName() + "] " + String.join(" | ", parts));
                    }
                }
            }
        }
        return String.join("\n", lines);
    }

    private String parseCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> rows = reader.lines().toList();
            if (rows.isEmpty()) {
                return "";
            }
            String[] headers = rows.get(0).split(",");
            List<String> lines = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                String[] cols = rows.get(i).split(",");
                List<String> parts = new ArrayList<>();
                for (int j = 0; j < Math.min(headers.length, cols.length); j++) {
                    String value = cols[j].trim();
                    if (!value.isEmpty()) {
                        parts.add(headers[j].trim() + ": " + value);
                    }
                }
                if (!parts.isEmpty()) {
                    lines.add(String.join(" | ", parts));
                }
            }
            return String.join("\n", lines);
        }
    }

    private String parseText(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
