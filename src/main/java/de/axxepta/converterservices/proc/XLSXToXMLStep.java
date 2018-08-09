package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ExcelUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

class XLSXToXMLStep extends Step {

    XLSXToXMLStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.XLSX_XML;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        String root = "xml";
        String sheetName = "Sheet1";
        boolean firstRowHead = true;
        boolean customXMLMapping = false;
        boolean indent = true;
        boolean columnFirst = false;
        String fileEl = "file";
        String sheetEl = "sheet";
        String rowEl = "row";
        String colEl = "column";
        String attSheetName = "";

        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1) {
                switch (parts[0].toLowerCase()) {
                    case "file":
                        fileEl = parts[1];
                        break;
                    case "root":
                        root = parts[1];
                        break;
                    case "sheet": case "sheetelement":
                        sheetEl = parts[1];
                        break;
                    case "sheetname":
                        sheetName = parts[1];
                        break;
                    case "column": case "col":
                        colEl = parts[1];
                        break;
                    case "row":
                        rowEl = parts[1];
                        break;
                    case "firstrowhead":
                        if (parts[1].toLowerCase().equals("false")) {
                            firstRowHead = false;
                        }
                        break;
                    case "indent":
                        if (parts[1].toLowerCase().equals("false")) {
                            indent = false;
                        }
                        break;
                    case "columnfirst":
                        if (parts[1].toLowerCase().equals("true")) {
                            columnFirst = true;
                        }
                    case "customxmlmapping": case "custom":
                        if (parts[1].toLowerCase().equals("true")) {
                            customXMLMapping = true;
                        }
                        break;
                }
            }
        }

        List<String> outputFiles = new ArrayList<>();
        if (inputFiles.size() > 0) {
            String inputFile = inputFiles.get(0);

            // if no external mapping file is provided use internal mapping or export all
            if (additional == null || (additional instanceof String && additional.equals(""))) {
                List<String> exportedFiles = ExcelUtils.fromPipeExcel(pipe.getWorkPath(), inputFile, ExcelUtils.FileType.XML,
                        customXMLMapping, sheetName, "", indent, columnFirst, false, true,
                        fileEl, sheetEl, rowEl, colEl, attSheetName);

                // transformation method has its own name pattern, rename if output name set
                if (!StringUtils.isNoStringOrEmpty(output) && exportedFiles.size() == 0) {
                    IOUtils.renameFile(exportedFiles.get(0), IOUtils.pathCombine(pipe.getWorkPath(), (String) output));
                }
                outputFiles.addAll(exportedFiles);
            } else {
                String mappingFile = resolveNotEmptyInput(additional, pipe).get(0);
                String nlSeparatedMappingList = IOUtils.loadStringFromFile(mappingFile);

                String xmlString = ExcelUtils.excelSheetTransformString(inputFile, sheetName, root, nlSeparatedMappingList,
                        firstRowHead);

                String outputFile = IOUtils.pathCombine(pipe.getWorkPath(), (StringUtils.isNoStringOrEmpty(output) ?
                        "step_" + pipe.getCounter() + "_sheet_" + sheetName + ".xml" : (String) output));
                IOUtils.saveStringToFile(xmlString, outputFile);
                outputFiles.add(outputFile);
            }

            pipe.addGeneratedFiles(outputFiles);
        } else {
            pipe.log("No input file in XLS_XML step!");
        }

        return outputFiles;
    }

    @Override
    boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
