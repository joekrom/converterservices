package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ExcelUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;

class XMLToCSVStep extends Step {

    XMLToCSVStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.XML_CSV;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        String[] parameterLines = StringUtils.nlListToArray((String) parameters);
        String row = "";
        String column = "";
        String delimiter = ";";
        for (String line : parameterLines) {
            String[] components = line.split(" *= *");
            if (components[0].equals(row)) {
                row = components[components.length - 1];
            }
            if (components[0].equals(column)) {
                column = components[components.length - 1];
            }
            if (components[0].equals(delimiter)) {
                delimiter = components[components.length - 1];
            }
        }
        String outputFile = getStandardOutputFile(pipe.getCounter(), pipe);
        try (ByteArrayOutputStream os = ExcelUtils.XMLToCSV(inputFiles.get(0), row, column, delimiter)) {
            IOUtils.ByteArrayOutputStreamToFile(os, outputFile);
        }
        pipe.addGeneratedFile(outputFile);
        return singleFileList(outputFile);
    }

    @Override
    boolean assertParameter(Parameter paramType, Object param) {
        return (param instanceof String);
    }
}
