package de.axxepta.converterservices.tools;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ExcelContentHandler extends DefaultHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(ExcelContentHandler.class);

    private static final String TEXT_CELL_TYPE = "text";
    private static final String DATE_CELL_TYPE = "date";
    private static final String NUM_CELL_TYPE = "number";

    private Sheet sheet;
    private CellStyle wrapStyle;
    private Font iFont;
    private Font bFont;
    private Font biFont;
    private Row row;
    private Cell cell;
    private String currentType = TEXT_CELL_TYPE;
    private StringBuilder cellContent = new StringBuilder();

    private String separator;
    private String rowTagName;
    private String colTagName;
    private final String typeAttName;
    private int rowCount = 0;
    private int colCount = 0;
    private boolean inCell = false;
    private int maxLines = 1;

    ExcelContentHandler(Workbook workbook, String sheetName, String rowTagName, String colTagName, String typeAttName,
                        String separator) {
        this.rowTagName = rowTagName;
        this.colTagName = colTagName;
        this.typeAttName = typeAttName;
        this.separator = separator;

        sheet = workbook.createSheet(sheetName);

        wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        iFont = workbook.createFont();
        iFont.setItalic(true);
        bFont = workbook.createFont();
        bFont.setBold(true);
        biFont = workbook.createFont();
        biFont.setBold(true);
        biFont.setItalic(true);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName.equals(rowTagName)) {
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
                    String[] lines = cellContent.toString().split(separator);
                    maxLines = Math.max(maxLines, lines.length);
                    String wrappedContent = String.join("\n", lines);
                    boolean cellFormat = true;
                    cell.setCellValue(cellFormat ?
                            formatRichText(wrappedContent) : new XSSFRichTextString(wrappedContent));
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
        content = getFormats(content, "i", formats, iFont);
        content = getFormats(content, "b", formats, bFont);


        XSSFRichTextString rtContent = new XSSFRichTextString(content);
        formats.forEach(c -> c.apply(rtContent));
/*        formats.sort(Comparator.comparing(RTFormat::getStart));
        for (int f = 0; f < formats.size(); f++) {
            if (f < formats.size() - 1 && formats.get(f).getEnd() > formats.get(f + 1).getEnd()) {
                // overlapping
            } else {
                formats.get(f).apply(rtContent);
            }
        }*/
        return rtContent;
    }

    private String getFormats(String content, String tag, List<RTFormat> formats, Font font) {
        int index = content.indexOf("<" + tag + ">");
        int index2 = content.indexOf("</" + tag + ">");
        while (index  != -1) {
            if (index2 != -1 && index2 > index) {
                formats.add(new RTFormat(index, index2 - 3, font));
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

        private void apply(RichTextString rts) {
            rts.applyFont(start, end, font);
        }
    }
}
