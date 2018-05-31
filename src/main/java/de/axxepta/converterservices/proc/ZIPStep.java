package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ZIPUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ZIPStep extends Step {

    ZIPStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.ZIP;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        String outputFile = pipe.getWorkPath() + (StringUtils.isEmpty(output) ? "step" + pipe.getCounter() + ".zip" : output);
        List<String> additionalInputs = new ArrayList<>();
        if ((additionalInput instanceof List) && ((List) additionalInput).get(0) instanceof String) {
            inputFiles.addAll((List<String>) additionalInput);
        } else if (additionalInput instanceof String) {
            additionalInputs.add((String) additionalInput);
        }
        try {
            ZIPUtils.zipRenamedFiles(outputFile, inputFiles, additionalInputs);
        } catch (IOException ex) {
            pipe.finalLogFileAdd(String.format("--- Exception zipping files in step %s: %s", pipe.getCounter(), ex.getMessage()));
        }
        pipe.addGeneratedFile(outputFile);
        return Arrays.asList(outputFile);
    }

    @Override
    boolean assertParameter(Parameter paramType, Object param) {
        if (StringUtils.isEmpty(param))
            return true;
        if (paramType.equals(Parameter.ADDITIONAL) && (param instanceof Pipeline.SubPipeline))
            return true;
        return (param instanceof String) || ((param instanceof List) && ((List) param).get(0) instanceof String);
    }
}
