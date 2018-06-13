package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ZIPUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

class UnzipStep extends Step {

    UnzipStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.UNZIP;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        List<String> outputFiles = new ArrayList<>();
        if ((additionalInput instanceof String) && (Boolean) parameters) {
            String outputFile = (output instanceof String) ? (String) output : (String) additionalInput;
            ZIPUtils.unzipSingle(inputFiles.get(0), (String) additionalInput, pipe.getWorkPath(), outputFile);
            outputFiles.add(outputFile);
        } else {
            outputFiles.addAll(ZIPUtils.unzip(inputFiles.get(0), pipe.getWorkPath()));
        }
        pipe.addGeneratedFiles(outputFiles);
        return outputFiles;
    }

    @Override
    boolean assertParameter(Parameter paramType, Object param) {
        if (StringUtils.isEmpty(param))
            return true;
        if (paramType.equals(Parameter.PARAMS)) {
            return param instanceof Boolean;
        }
        return (param instanceof String);
    }
}
