package de.axxepta.converterservices.proc;

import java.util.List;

class EmptyStep extends Step {

    EmptyStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.NONE;
    }

    @Override
    Object execAction(List<String> inputFiles, String... parameters) throws Exception {
        return inputFiles;
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        return true;
    }
}
