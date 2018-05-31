package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

class XSLTStep extends Step {

    XSLTStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.XSLT;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        String inputFile = inputFiles.get(0);
        String outputFile = pipe.getWorkPath() + (StringUtils.isEmpty(output) ?
                Saxon.standardOutputFilename((String) additionalInput) : (String) output);
        pipe.saxonTransform(inputFile, pipe.getInputPath() + additionalInput,
                outputFile, parameters instanceof String ? (String) parameters : "");
        pipe.logFileAddArray(pipe.getErrFileArray());
        pipe.finalLogFileAdd(inputFile + ": " + pipe.getErrFileArray().getSize() + " messages");
        pipe.incErrorCounter(pipe.getErrFileArray().getSize());
        pipe.addLogSectionXsl(inputFile, pipe.getErrFileArray());
        pipe.addGeneratedFile(outputFile);
        List<String> outputFiles = new ArrayList<>();
        outputFiles.add(outputFile);
        return outputFiles;
    }

    @Override
    boolean assertParameter(Parameter paramType, Object param) {
        if (paramType.equals(Parameter.ADDITIONAL) && StringUtils.isEmpty(param))
            return false;
        return (param instanceof String) || ((param instanceof List) && ((List) param).get(0) instanceof String);
    }
}
