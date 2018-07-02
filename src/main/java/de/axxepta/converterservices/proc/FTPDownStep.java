package de.axxepta.converterservices.proc;

import java.util.List;

class FTPDownStep extends Step {

    FTPDownStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.FTP_DOWN;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        return null;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
