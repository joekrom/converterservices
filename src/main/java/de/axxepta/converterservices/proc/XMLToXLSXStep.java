package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.Const;
import de.axxepta.converterservices.tools.ExcelUtils;
import de.axxepta.converterservices.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

class XMLToXLSXStep extends Step {

    XMLToXLSXStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.XML_XLSX;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        String row = Const.ROW_TAG;
        String column = Const.CELL_TAG;
        String sheet = Const.SHEET_NAME;
        String dataType = Const.DATA_TYPE_ATT;
        String separator = ExcelUtils.XML_SEPARATOR;
        boolean cellFormat = true;

        for (String line : parameters) {
            String[] parts = line.split(" *= *");
            if (parts.length > 1) {
                switch (parts[0].toLowerCase()) {
                    case "tr": case "row":
                        row = parts[1];
                        break;
                    case "td": case "column":
                        column = parts[1];
                        break;
                    case "sheetname":
                        sheet = parts[1];
                        break;
                    case "data": case "type": case "data-type": case "datatype":
                        dataType = parts[1];
                        break;
                    case "cellformat": case "format": case "cell-format":
                        if (parts[1].toLowerCase().equals("false")) {
                            cellFormat = false;
                        }
                        break;
                }
            }
        }

        List<String> providedOutputNames = listifyOutput(pipe);
        List<String> usedOutputFiles = new ArrayList<>();
        int i = 0;
        for (String inFile : inputFiles) {
            String outputFile = getCurrentOutputFile(providedOutputNames, i, inFile, pipe);
            ExcelUtils.XMLToExcel(inFile, sheet, row, column, dataType, cellFormat, separator, outputFile);
            pipe.addGeneratedFile(outputFile);
            usedOutputFiles.add(outputFile);
            i++;
        }
        return usedOutputFiles;
    }

    private String getCurrentOutputFile(List<String> providedOutputNames, int current, String inputFile, Pipeline pipe) {
        return providedOutputNames.size() > current && !providedOutputNames.get(current).equals("") ?
                IOUtils.pathCombine(pipe.getWorkPath(), providedOutputNames.get(current)) :
                IOUtils.pathCombine(pipe.getWorkPath(),IOUtils.filenameFromPath(inputFile) + ".xlsx");
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return paramType.equals(Parameter.ADDITIONAL)|| paramType.equals(Parameter.PARAMS)|| assertStandardInput(param);
    }
}
