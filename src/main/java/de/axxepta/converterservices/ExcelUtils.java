package de.axxepta.converterservices;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;

class ExcelUtils {

    private static Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

    private static final String XML_PROLOGUE    = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>"
            + System.lineSeparator();
    private static final String SHEET_INDENT    = "  ";
    private static final String ROW_INDENT      = "    ";
    private static final String COL_INDENT      = "      ";
    private static final String CONTENT_INDENT  = "        ";
    private static final String FILE_EL         = "workbook";
    static final String SHEET_EL                = "sheet";
    static final String ROW_EL                  = "row";
    static final String COL_EL                  = "column";
    static final String DEF_SHEET_NAME          = "sheet0";
    static final String DEF_ATT_SHEET           = "id";
    static final String DEF_SEPARATOR           = ";";

    private ExcelUtils() {}


    static List<String> fromExcel(String fileName, FileType type, String sheetName, String separator,
                          boolean indent, boolean columnFirst, boolean firstColName, boolean firstRowName,
                          String fileEl, String sheetEl, String rowEl, String colEl) {
        List<String> outputFiles = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(App.TEMP_FILE_PATH + "/" + fileName)) {
            Workbook workbook = new XSSFWorkbook(file);
            if (type.equals(FileType.CSV)) {
                outputFiles.addAll(excelToCSV(fileName, workbook, sheetName, separator));
            }
            if (type.equals(FileType.XML)) {
                outputFiles.add(excelToXML(fileName, workbook, sheetName, columnFirst, firstColName, firstRowName,
                        fileEl, sheetEl, rowEl, colEl, indent));
            }
        } catch (IOException ie) {
            logger.error("Exception reading Excel file: " + ie.getMessage());
        }
        return outputFiles;
    }

    private static List<String> excelToCSV(String fileName, Workbook workbook, String sheetName, String separator) {
        List<String> outputFiles = new ArrayList<>();
        List<Sheet> sheets = getSheets(workbook, sheetName);
        DataFormatter formatter = new DataFormatter(true);
        for (Sheet sheet : sheets) {
            String convertedFileName = csvFileName(fileName, sheet.getSheetName());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(App.TEMP_FILE_PATH + "/" + convertedFileName))) {
                int firstRow = sheet.getFirstRowNum();
                int lastRow = sheet.getLastRowNum();
                int firstColumn = Math.min(sheet.getRow(firstRow).getFirstCellNum(), sheet.getRow(firstRow + 1).getFirstCellNum());
                int lastColumn = Math.max(sheet.getRow(firstRow).getLastCellNum(), sheet.getRow(firstRow + 1).getLastCellNum());
                for (int rowNumber = firstRow; rowNumber < lastRow + 1; rowNumber++) {
                    Row row = sheet.getRow(rowNumber);
                    StringJoiner joiner = new StringJoiner(separator);
                    for (int colNumber = firstColumn; colNumber < lastColumn; colNumber++) {
                        Cell cell = row.getCell(colNumber);
                        joiner.add(formatter.formatCellValue(cell));
                    }
                    writer.write(joiner.toString() + System.lineSeparator());
                }
            } catch (IOException ie) {
                logger.error("Exception writing to CSV file: " + ie.getMessage());
            }
            outputFiles.add(convertedFileName);
        }
        return outputFiles;
    }

    private static String csvFileName(String xlsName, String sheetName) {
        return xlsName.substring(0, xlsName.lastIndexOf(".")) + "_" + sheetName + ".csv";
    }

    static String csvToExcel(String path) {
        Scanner scanner;
        return "";
    }

    static String xmlToExcel(String path) {
        return "";
    }

    private static String xmlFileName(String xlsName) {
        return xlsName.substring(0, xlsName.lastIndexOf(".")) + ".xml";
    }

    private static String excelToXML(String fileName, Workbook workbook, String sheetName, boolean columnFirst,
                                   boolean firstColName, boolean firstRowName,
                                   String fileEl, String sheetEl, String rowEl, String colEl,
                                   boolean indent) {
        List<Sheet> sheets = getSheets(workbook, sheetName);
        DataFormatter formatter = new DataFormatter(true);

        String outputFile = xmlFileName(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(App.TEMP_FILE_PATH + "/" + outputFile))) {
            writer.write(XML_PROLOGUE);
            writeTag(writer, TagType.open, fileEl.equals("") ? FILE_EL : fileEl, indent, "");
            for (Sheet sheet : sheets) {
                writeTag(writer, TagType.open, sheetEl.equals("") ? SHEET_EL : sheetEl, indent, SHEET_INDENT);
                int firstRow = sheet.getFirstRowNum();
                int lastRow = sheet.getLastRowNum();
                int firstColumn = Math.min(sheet.getRow(firstRow).getFirstCellNum(), sheet.getRow(firstRow + 1).getFirstCellNum());
                int lastColumn = Math.max(sheet.getRow(firstRow).getLastCellNum(), sheet.getRow(firstRow + 1).getLastCellNum());
                if (columnFirst) {
                    //
                } else {
                    for (int rowNumber = firstRow; rowNumber < lastRow + 1; rowNumber++) {
                        Row row = sheet.getRow(rowNumber);
                        writeTag(writer, TagType.open, rowEl.equals("") ? ROW_EL : rowEl, indent, ROW_INDENT);
                        for (int colNumber = firstColumn; colNumber < lastColumn; colNumber++) {
                            Cell cell = row.getCell(colNumber);
                            writeElement(writer, colEl.equals("") ? COL_EL : colEl,
                                    formatter.formatCellValue(cell), indent, CONTENT_INDENT, COL_INDENT);
                        }
                        writeTag(writer, TagType.close, rowEl.equals("") ? ROW_EL : rowEl, indent, ROW_INDENT);
                    }
                }
                writeTag(writer, TagType.close, sheetEl.equals("") ? SHEET_EL : sheetEl, indent, SHEET_INDENT);
            }
            writeTag(writer, TagType.close, fileEl.equals("") ? FILE_EL : fileEl, indent, "");
        } catch (IOException ie){
            logger.error("Exception writing to XML file: " + ie.getMessage());
        }
        return outputFile;
    }

    private static List<Sheet> getSheets(Workbook workbook, String sheetName) {
        List<Sheet> sheets = new ArrayList<>();
        if (!sheetName.equals("") && (workbook.getSheet(sheetName) != null)) {
            sheets.add(workbook.getSheet(sheetName));
        } else {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                sheets.add(workbook.getSheetAt(s));
            }
        }
        return sheets;
    }

    private static void writeTag(BufferedWriter writer, TagType tag, String name,
                                 boolean indent, String indentString) throws IOException {
        String s = (indent ? indentString : "").
                concat((tag.equals(TagType.open)) ? "<" + name + ">" : "</" + name + ">").
                concat(indent ? System.lineSeparator() : "");
        writer.write(s);
    }

    private static void writeElement(BufferedWriter writer, String name, String content,
                                 boolean indent, String indentContentString, String indentTagString) throws IOException {
        writeTag(writer, TagType.open, name, indent, indentTagString);
        String s = (indent ? indentContentString : "").concat(content).concat(indent ? System.lineSeparator() : "");
        writer.write(s);
        writeTag(writer, TagType.close, name, indent, indentTagString);
    }


    public enum FileType {
        XLSX,
        CSV,
        XML
    }

    private enum TagType {
        open,
        close
    }
}
