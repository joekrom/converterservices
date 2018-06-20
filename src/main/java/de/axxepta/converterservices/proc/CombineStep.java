package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

class CombineStep extends Step {

    CombineStep(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.COMBINE;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final Object additionalInput, final String... parameters)
            throws Exception
    {
        List<String> outputFiles = new ArrayList<>();
        outputFiles.addAll(inputFiles);
        if (additionalInput instanceof String) {
            outputFiles.add(pipedPath(additionalInput, pipe));
        } else {
            for (Object inFile : (List) additionalInput) {
                outputFiles.add(pipedPath(inFile, pipe));
            }
        }
        actualOutput = outputFiles;
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return (paramType.equals(Parameter.ADDITIONAL) && StringUtils.isEmpty(param)) ||
                (!paramType.equals(Parameter.ADDITIONAL) && assertStandardInput(param));
    }
}
