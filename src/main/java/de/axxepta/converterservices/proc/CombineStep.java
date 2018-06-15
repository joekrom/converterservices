package de.axxepta.converterservices.proc;


import de.axxepta.converterservices.utils.StringUtils;

import java.util.List;

class CombineStep extends Step {

    CombineStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.COMBINE;
    }

    @Override
    Object execAction(final List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        if (additionalInput instanceof String) {
            inputFiles.add(pipedPath(additionalInput, pipe));
        } else {
            for (Object inFile : (List) additionalInput) {
                inputFiles.add(pipedPath(inFile, pipe));
            }
        }
        actualOutput = inputFiles;
        return inputFiles;
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        if (paramType.equals(Parameter.ADDITIONAL) && StringUtils.isEmpty(param))
            return false;
        if (paramType.equals(Parameter.ADDITIONAL) && (param instanceof Pipeline.SubPipeline))
            return true;
        return (param instanceof String) || (param instanceof Integer) ||
                ((param instanceof List) && ((List) param).get(0) instanceof String);
    }
}
