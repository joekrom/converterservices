package de.axxepta.converterservices.tools;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

import static org.apache.poi.ss.usermodel.Font.U_SINGLE;

public class ExcelContentHandler extends DefaultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelContentHandler.class);

    private static final String TEXT_CELL_TYPE = "text";
    private static final String DATE_CELL_TYPE = "date";
    private static final String NUM_CELL_TYPE = "number";
    private static final String SHEET_ATTR = "sheet";

    private final boolean multiSheet;
    private final Workbook workbook;
    private Sheet sheet;
    private final CellStyle wrapStyle;
    private Row row;
    private Cell cell;
    private String currentType = TEXT_CELL_TYPE;
    private StringBuilder cellContent = new StringBuilder();

    private final boolean cellFormat;
    private final String sheetTagName;
    private final String separator;
    private final String rowTagName;
    private final String colTagName;
    private final String typeAttName;
    private int sheetCount = 0;
    private int rowCount = 0;
    private int colCount = 0;
    private boolean inCell = false;
    private int maxLines = 1;

    private static final Map<String, Integer> formatCodes = new HashMap<>();
    private final Map<Object, Object> formatAssignments = new HashMap<>();

    ExcelContentHandler(Workbook workbook, boolean multiSheet, String sheetName, String rowTagName, String colTagName, String typeAttName,
                        boolean cellFormat, String separator) {
        this.workbook = workbook;
        this.multiSheet = multiSheet;
        this.sheetTagName = sheetName;
        this.rowTagName = rowTagName;
        this.colTagName = colTagName;
        this.typeAttName = typeAttName;
        this.cellFormat = cellFormat;
        this.separator = separator;

        if (!multiSheet) {
            sheet = workbook.createSheet(sheetName);
        }

        wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);

        Font iFont = workbook.createFont();
        iFont.setItalic(true);
        Font bFont = workbook.createFont();
        bFont.setBold(true);
        Font biFont = workbook.createFont();
        biFont.setBold(true); biFont.setItalic(true);
        Font uFont = workbook.createFont();
        uFont.setUnderline(U_SINGLE);
        Font ubFont = workbook.createFont();
        ubFont.setUnderline(U_SINGLE); ubFont.setBold(true);
        Font uiFont = workbook.createFont();
        uiFont.setUnderline(U_SINGLE); uiFont.setItalic(true);
        Font ubiFont = workbook.createFont();
        ubiFont.setUnderline(U_SINGLE); ubiFont.setItalic(true); ubiFont.setBold(true);

        if (formatCodes.size() == 0) {
            synchronized (formatCodes) {
                if (formatCodes.size() == 0) {
                    formatCodes.put("i", 1);
                    formatCodes.put("em", 1);
                    formatCodes.put("b", 2);
                    formatCodes.put("strong", 2);
                    formatCodes.put("u", 4);
                }
            }
        }

        formatAssignments.put(1, iFont);
        formatAssignments.put(iFont, 1);
        formatAssignments.put(2, bFont);
        formatAssignments.put(bFont, 2);
        formatAssignments.put(3, biFont);
        formatAssignments.put(4, uFont);
        formatAssignments.put(uFont, 4);
        formatAssignments.put(6, ubFont);
        formatAssignments.put(5, uiFont);
        formatAssignments.put(7, ubiFont);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (multiSheet && qName.equals(sheetTagName)) {
            String sheetName = attributes.getValue(SHEET_ATTR);
            if (sheetName == null) {
                sheetName = sheetTagName + sheetCount++;
            }
            sheet = workbook.createSheet(sheetName);
            rowCount = 0;
        } else if (qName.equals(rowTagName)) {
            row = sheet.createRow(rowCount++);
            colCount = 0;
        } else if (qName.equals(colTagName)) {
            cell = row.createCell(colCount++);
            currentType = attributes.getValue(typeAttName);
            if (currentType != null && currentType.equals(TEXT_CELL_TYPE)) {
                cell.setCellStyle(wrapStyle);
            }
            inCell = true;
        } else if (inCell) {
            cellContent.append("<").append(qName);
            for (int a = 0; a < attributes.getLength(); a++) {
                cellContent.append(" ").append(attributes.getQName(a)).append("=\"").append(attributes.getValue(a)).append("\"");
            }
            cellContent.append(">");
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals(colTagName)) {
            if (currentType == null) {
                currentType = "text";
            }
            switch (currentType.toLowerCase()) {
                case DATE_CELL_TYPE:
                    cell.setCellValue(cellContent.toString());
                    break;
                case NUM_CELL_TYPE:
                case "numeric":
                    if (cellContent.length() > 0) {
                        String c = cellContent.toString();
                        try {
                            if (c.contains(".") || c.contains(",")) {
                                c = c.replace(",", ".");
                                double val = Double.parseDouble(c);
                                cell.setCellValue(val);
                            } else {
                                int val = Integer.parseInt(c);
                                cell.setCellValue(val);
                            }
                        } catch (NumberFormatException nf) {
                            LOGGER.warn(String.format("Couldn't parse numeric value during conversion to Excel in row %s, col %s", rowCount, colCount));
                        }
                    }
                    break;
                default:    // text
                    if (cellContent.length() > 0) {
                        String[] lines = cellContent.toString().split(separator);
                        maxLines = Math.max(maxLines, lines.length);
                        String wrappedContent = String.join("\n", lines);
                        cell.setCellValue(cellFormat ?
                                formatRichText(wrappedContent) : new XSSFRichTextString(wrappedContent));
                    }
            }
            inCell = false;
            cellContent = new StringBuilder();
        } else if (qName.equals(rowTagName)) {
            row.setHeightInPoints((maxLines * sheet.getDefaultRowHeightInPoints()));
            maxLines = 1;
        } else if (inCell) {
            cellContent.append("</").append(qName);
            cellContent.append(">");
        }
    }

    private XSSFRichTextString formatRichText(String content) {
        List<RTFormat> formats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : formatCodes.entrySet()) {
            content = getFormats(content, entry.getKey(), formats, (Font) formatAssignments.get(entry.getValue()));
        }
        checkFormatOverlap(content.length(), formats);
        XSSFRichTextString rtContent = new XSSFRichTextString(content);
        formats.forEach(c -> c.apply(rtContent));
        return rtContent;
    }

    private String getFormats(String content, String tag, List<RTFormat> formats, Font font) {
        int index = content.indexOf("<" + tag + ">");
        int index2 = content.indexOf("</" + tag + ">");
        while (index  != -1) {
            if (index2 != -1 && index2 > index) {
                for (RTFormat format : formats) {
                    if (format.getEnd() > index2) {
                        format.shiftEnd(tag.length() + 3);
                    }
                    if (format.getEnd() > index) {
                        format.shiftEnd(tag.length() + 2);
                    }
                    if (format.getStart() > index2) {
                        format.shiftStart(tag.length() + 3);
                    }
                    if (format.getStart() > index) {
                        format.shiftStart(tag.length() + 2);
                    }
                }
                formats.add(new RTFormat(index, index2 - (tag.length() + 2), font));
                content = content.replaceFirst("<" + tag + ">", "");
                content = content.replaceFirst("</" + tag + ">", "");
            } else {
                break;
            }
            index = content.indexOf("<" + tag + ">");
            index2 = content.indexOf("</" + tag + ">");
        }
        return content;
    }

    private void checkFormatOverlap(int contentLength, List<RTFormat> formats) {
        int[] charFormats = new int[contentLength];
        formats.forEach(f -> {
            for (int c = f.getStart(); c < f.getEnd(); c++) {
                charFormats[c] += (Integer) formatAssignments.get(f.getFont());
            }
        });
        formats.clear();
        int currentFormat = charFormats[0];
        int currentStart = 0;
        for (int c = 0; c < contentLength; c++) {
            if (c == contentLength - 1 || currentFormat != charFormats[c + 1]) {
                if (currentFormat != 0) {
                    formats.add(new RTFormat(currentStart, c + 1, (Font) formatAssignments.get(currentFormat)));
                }
                currentStart = c + 1;
            }
            if (c < contentLength - 1) {
                currentFormat = charFormats[c + 1];
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (inCell) {
            String content = new String(Arrays.copyOfRange(ch, start, start + length));
            cellContent.append(content);
        }
    }

    private static class RTFormat {
        int start;
        int end;
        Font font;

        private RTFormat(int start, int end, Font font) {
            this.start = start;
            this.end = end;
            this.font = font;
        }

        private int getStart() {
            return start;
        }

        private int getEnd() {
            return end;
        }

        private Font getFont() {
            return font;
        }

        private void shiftStart(int s) {
            start -= s;
        }

        private void shiftEnd(int s) {
            end -= s;
        }

        private void apply(RichTextString rts) {
            rts.applyFont(start, end, font);
        }
    }
}
