package de.axxepta.converterservices.proc;


import java.util.List;

class PDFSplitStep extends Step {

    PDFSplitStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.PDF_SPLIT;
    }

    @Override
    Object execAction(Pipeline pipe, List<String> inputFiles, String... parameters) throws Exception {
        return null;
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        if (paramType.equals(Parameter.PARAMS) && !(param instanceof Boolean))
            return false;
        return (param instanceof String) || ((param instanceof List) && ((List) param).get(0) instanceof String);
    }
}
