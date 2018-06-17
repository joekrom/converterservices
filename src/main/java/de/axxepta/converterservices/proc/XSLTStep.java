package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.List;

class XSLTStep extends Step {

    XSLTStep(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.XSLT;
    }

    @Override
    Object execAction(Pipeline pipe, List<String> inputFiles, Object additionalInput, String... parameters) throws Exception {
        String inputFile = inputFiles.get(0);
        String outputFile = pipe.getWorkPath() + (StringUtils.isEmpty(output) ?
                Saxon.standardOutputFilename((String) additionalInput) : (String) output);
        pipe.saxonTransform(inputFile, pipe.getInputPath() + additionalInput,
                outputFile, parameters);
        pipe.logFileAddArray(pipe.getErrFileArray());
        pipe.finalLogFileAdd(inputFile + ": " + pipe.getErrFileArray().getSize() + " messages");
        pipe.incErrorCounter(pipe.getErrFileArray().getSize());
        pipe.addLogSectionXsl(inputFile, pipe.getErrFileArray());
        pipe.addGeneratedFile(outputFile);
        actualOutput = outputFile;
        return singleFileList(outputFile);
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        return true;
/*        if (paramType.equals(Parameter.ADDITIONAL) && StringUtils.isEmpty(param))
            return false;
        return (param instanceof String) || ((param instanceof List) && ((List) param).get(0) instanceof String);*/
    }
}
