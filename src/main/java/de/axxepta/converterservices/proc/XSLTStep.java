package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.List;

class XSLTStep extends Step {

    XSLTStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.XSLT;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        String inputFile = inputFiles.get(0);
        String outputFile = pipe.getWorkPath() + (StringUtils.isNoStringOrEmpty(output) ?
                Saxon.standardOutputFilename((String) additional) : (String) output);
        pipe.saxonTransform(inputFile, pipedPath(additional, pipe), outputFile, parameters);
        pipe.logFileAddArray(pipe.getErrFileArray());
        pipe.finalLogFileAdd(inputFile + ": " + pipe.getErrFileArray().getSize() + " messages");
        pipe.incErrorCounter(pipe.getErrFileArray().getSize());
        pipe.addLogSectionXsl(inputFile, pipe.getErrFileArray());
        pipe.addGeneratedFile(outputFile);
        return singleFileList(outputFile);
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return !paramType.equals(Parameter.INPUT)|| assertStandardInput(param);
    }
}
