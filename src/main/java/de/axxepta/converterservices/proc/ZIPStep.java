package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ZIPUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ZIPStep extends Step {

    ZIPStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.ZIP;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        String outputFile = IOUtils.pathCombine(pipe.getWorkPath(),
                StringUtils.isNoStringOrEmpty(output) ? "step" + pipe.getCounter() + ".zip" : (String) output);
        List<String> additionalInputs = new ArrayList<>();
        if ((additional instanceof List) && ((List) additional).get(0) instanceof String) {
            additionalInputs.addAll((List<String>) additional);
        } else if (additional instanceof String && !additional.equals("")) {
            additionalInputs.add((String) additional);
        } else {
            for (String inputFile : inputFiles) {
                additionalInputs.add(IOUtils.relativePath(inputFile, pipe.getWorkPath()));
            }
        }
        try {
            ZIPUtils.zipRenamedFiles(outputFile, inputFiles, additionalInputs);
        } catch (IOException ex) {
            pipe.finalLogFileAdd(String.format("--- Exception zipping files in step %s: %s", pipe.getCounter(), ex.getMessage()));
        }
        pipe.addGeneratedFile(outputFile);
        return singleFileList(outputFile);
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        switch (paramType) {
            case INPUT:
                return assertStandardInput(param);
            case OUTPUT:
                return param == null || param instanceof String;
            case ADDITIONAL:
                return assertStandardInput(param);
            default: return true;
        }
    }
}
