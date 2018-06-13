package de.axxepta.converterservices.proc;

import java.util.List;

class MD5FilterStep extends Step {

    MD5FilterStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.MD5_FILTER;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        return null;
    }

    @Override
    boolean assertParameter(Parameter paramType, Object param) {
        return true;
    }
}
