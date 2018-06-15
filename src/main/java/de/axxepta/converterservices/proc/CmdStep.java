package de.axxepta.converterservices.proc;

import java.util.List;

class CmdStep extends Step {

    CmdStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return null;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        return null;
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        return true;
    }
}
