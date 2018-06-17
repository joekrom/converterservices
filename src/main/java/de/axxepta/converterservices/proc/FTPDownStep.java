package de.axxepta.converterservices.proc;

import java.util.List;

class FTPDownStep extends Step {

    FTPDownStep(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.FTP_DOWN;
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
