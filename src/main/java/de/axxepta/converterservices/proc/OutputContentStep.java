package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OutputContentStep extends Step {

    OutputContentStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.OUTPUT_CONTENT;
    }

    @Override
    Object execAction(final List<String> inputFiles, final String... parameters) throws Exception {
        String basePath = "";
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1 && parts[0].toLowerCase().contains("base")) {
                basePath = parts[1];
            }
        }

        List<String> outputFileNames = new ArrayList<>();
        for (String inFile : inputFiles) {
            try {
                List<String> lines = IOUtils.loadStringsFromFile(inFile);
                for (String line : lines) {
                    outputFileNames.add(
                            basePath.equals("") ? line :
                                    (basePath.equals("pipe") ? IOUtils.pathCombine(pipe.getWorkPath(), line) :
                                            IOUtils.pathCombine(basePath, line))
                    );
                }
            } catch (IOException ie) {
                pipe.log(String.format("Error reading input text file %s: ", ie.getMessage()));
            }
        }

        return outputFileNames;
    }

    @Override
    boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
