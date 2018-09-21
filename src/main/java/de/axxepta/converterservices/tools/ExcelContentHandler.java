package de.axxepta.converterservices.tools;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Arrays;

public class ExcelContentHandler extends DefaultHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(ExcelContentHandler.class);

    private static final String TEXT_CELL_TYPE = "text";
    private static final String DATE_CELL_TYPE = "date";
    private static final String NUM_CELL_TYPE = "number";

    private Sheet sheet;
    private CellStyle wrapStyle;
    private Row row;
    private Cell cell;
    private String currentType = TEXT_CELL_TYPE;
    private StringBuilder cellContent = new StringBuilder();

    private String separator;
    private String rowTagName;
    private String colTagName;
    private String typeAttName;
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
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName.equals(rowTagName)) {
            row = sheet.createRow(rowCount++);
            colCount = 0;
        }
        if (qName.equals(colTagName)) {
            cell = row.createCell(colCount++);
            currentType = attributes.getValue(typeAttName);
            if (currentType != null && currentType.equals(TEXT_CELL_TYPE)) {
                cell.setCellStyle(wrapStyle);
            }
            inCell = true;
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
                default:
                    String[] lines = cellContent.toString().split(separator);
                    maxLines = Math.max(maxLines, lines.length);
                    cell.setCellValue(new XSSFRichTextString( String.join("\n", lines) ));
            }
            inCell = false;
            cellContent = new StringBuilder();
        }
        if (qName.equals(rowTagName)) {
            row.setHeightInPoints((maxLines * sheet.getDefaultRowHeightInPoints()));
            maxLines = 1;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (inCell) {
            String content = new String(Arrays.copyOfRange(ch, start, start + length));
            cellContent.append(content);
        }
    }
}
