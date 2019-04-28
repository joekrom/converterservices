package de.axxepta.converterservices.proc;

import java.util.ArrayList;
import java.util.List;

class CombineStep extends Step {

    CombineStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.COMBINE;
    }

    @Override
    Object execAction(final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        List<String> outputFiles = new ArrayList<>();
        outputFiles.addAll(inputFiles);
        outputFiles.addAll(resolveInput(additional, true));
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return paramType.equals(Parameter.PARAMS) || assertStandardInput(param);
    }
}
