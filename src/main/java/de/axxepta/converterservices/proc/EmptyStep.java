package de.axxepta.converterservices.proc;

import java.util.List;

class EmptyStep extends Step {

    EmptyStep(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.NONE;
    }

    @Override
    Object execAction(Pipeline pipe, List<String> inputFiles, Object additionalInput, String... parameters) throws Exception {
        return null;
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        return true;
    }
}
