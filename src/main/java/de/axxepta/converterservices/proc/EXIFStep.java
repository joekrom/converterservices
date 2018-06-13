package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.CmdUtils;
import de.axxepta.converterservices.utils.IOUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;

class EXIFStep extends Step {

    EXIFStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.EXIF;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        boolean compact = (parameters instanceof Boolean) && (Boolean) parameters;
        String outputFile = getStandardOutputFile(pipe.getCounter(), pipe);
        try (ByteArrayOutputStream os = CmdUtils.exif(compact, "-X", inputFiles.get(0)) ) {
            IOUtils.ByteArrayOutputStreamToFile(os, outputFile);
        }
        pipe.addGeneratedFile(outputFile);
        return singleFileList(outputFile);
    }

    @Override
    boolean assertParameter(Parameter paramType, Object param) {
        return (param instanceof Boolean);
    }

}
