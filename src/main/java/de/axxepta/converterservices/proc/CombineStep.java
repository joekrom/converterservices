package de.axxepta.converterservices.proc;

import java.util.ArrayList;
import java.util.List;

class CombineStep extends Step {

    CombineStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.COMBINE;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        List<String> outputFiles = new ArrayList<>();
        outputFiles.addAll(inputFiles);
        if (additional instanceof String) {
            outputFiles.add(pipedPath(additional, pipe));
        } else {
            for (Object inFile : (List) additional) {
                outputFiles.add(pipedPath(inFile, pipe));
            }
        }
        actualOutput = outputFiles;
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return paramType.equals(Parameter.PARAMS) || assertStandardInput(param);
    }
}
