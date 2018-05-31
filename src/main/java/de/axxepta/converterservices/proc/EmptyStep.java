package de.axxepta.converterservices.proc;

import java.util.List;

class EmptyStep extends Step {

    EmptyStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.NONE;
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
