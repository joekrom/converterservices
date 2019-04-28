package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ExcelUtils;
import de.axxepta.converterservices.utils.IOUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

class XMLToCSVStep extends Step {

    XMLToCSVStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.XML_CSV;
    }

    @Override
    Object execAction(final List<String> inputFiles, final String... parameters)
            throws Exception
    {
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

        List<String> providedOutputNames = listifyOutput(pipe);
        List<String> usedOutputFiles = new ArrayList<>();
        int i = 0;
        for (String inFile : inputFiles) {
            String outputFile = getCurrentOutputFile(providedOutputNames, i, inFile);

            try (ByteArrayOutputStream os = ExcelUtils.XMLToCSV(inFile, row, column, delimiter)) {
                IOUtils.byteArrayOutputStreamToFile(os, outputFile);
            }
            pipe.addGeneratedFile(outputFile);
            usedOutputFiles.add(outputFile);
            i++;
        }
        return usedOutputFiles;
    }

    private String getCurrentOutputFile(List<String> providedOutputNames, int current, String inputFile) {
        return providedOutputNames.size() > current && !providedOutputNames.get(current).equals("") ?
                IOUtils.pathCombine(pipe.getWorkPath(), providedOutputNames.get(current)) :
                IOUtils.pathCombine(pipe.getWorkPath(),IOUtils.filenameFromPath(inputFile) + ".csv");
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return paramType.equals(Parameter.ADDITIONAL)|| paramType.equals(Parameter.PARAMS)|| assertStandardInput(param);
    }
}
