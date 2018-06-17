package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ExcelUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

class XMLToCSVStep extends Step {

    XMLToCSVStep(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.XML_CSV;
    }

    @Override
    Object execAction(Pipeline pipe, List<String> inputFiles, Object additionalInput, String... parameters) throws Exception {
        String row = "tr";
        String column = "td";
        String delimiter = ";";
        for (String line : parameters) {
            String[] components = line.split(" *= *");
            if (components[0].toLowerCase().equals("tr") || components[0].toLowerCase().equals("row")) {
                row = components[components.length - 1];
            }
            if (components[0].toLowerCase().equals("td") || components[0].toLowerCase().equals("column")) {
                column = components[components.length - 1];
            }
            if (components[0].toLowerCase().startsWith("delim") || components[0].toLowerCase().startsWith("sep")) {
                delimiter = components[components.length - 1];
            }
        }

        List<String> outputFiles = new ArrayList<>();
        String outputFile;
        int i = 0;
        for (String inFile : inputFiles) {
            if ((output instanceof List) && (((List) output).size() == inputFiles.size())) {
                outputFile = IOUtils.pathCombine(pipe.getWorkPath(), (String) ((List) output).get(i));
            } else if (StringUtils.isEmpty(output)) {
                outputFile = IOUtils.filenameFromPath(inFile) + ".csv";
            } else {
                outputFile = IOUtils.pathCombine(pipe.getWorkPath(), (String) output);
            }

            try (ByteArrayOutputStream os = ExcelUtils.XMLToCSV(inFile, row, column, delimiter)) {
                IOUtils.ByteArrayOutputStreamToFile(os, outputFile);
            }
            pipe.addGeneratedFile(outputFile);
            outputFiles.add(outputFile);
            i++;
        }
        actualOutput = outputFiles;
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        if (paramType.equals(additional)) {
            return true;
        } else {
            return param == null || param instanceof String ||
                    (param instanceof List && ((List) param).get(0) instanceof String);
        }
    }
}
