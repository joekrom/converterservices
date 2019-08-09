package de.axxepta.converterservices;

public class Const {

    private Const() {}

    public static final String STATIC_FILE_PATH = System.getProperty("user.home") + "/.converterservices";
    public static final String TEMP_FILE_PATH = STATIC_FILE_PATH + "/temp";

    public static final String DATA_TYPE_ATT            = "data-type";
    public static final String TYPE_JPEG                = "image/jpeg";
    public static final String TYPE_PNG                 = "image/png";
    public static final String TYPE_PDF                 = "application/pdf";
    public static final String TYPE_XLSX                = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String TYPE_TEXT                = "text/plain";
    public static final String TYPE_CSV                 = "text/csv";
    public static final String TYPE_XML                 = "application/xml";
    public static final String TYPE_OCTET               = "application/octet-stream";

    public static final String SHEET_NAME = "Sheet1";

    public static final String SHEET_TAG = "sheet";
    public static final String ROW_TAG = "Row";
    public static final String CELL_TAG = "Cell";

}
