package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.CmdUtils;
import de.axxepta.converterservices.utils.IOUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

class CmdStep extends Step {

    CmdStep(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return null;
    }

    @Override
    Object execAction(Pipeline pipe, List<String> inputFiles, Object additionalInput, String... parameters) throws Exception {
        String cmdLine = ((String[]) parameters)[0];
        List<String> outputFiles = new ArrayList<>();
        int i = 0;
        for (String inFile : inputFiles) {
            int outSize = 0;
            List<String> outputs = new ArrayList<>();
            try {
                outputs = (List) output;
                outSize = outputs.size();
            } catch (ClassCastException cc) {}
            String outputFile = (inputFiles.size() == outSize) ?
                    IOUtils.pathCombine(pipe.getWorkPath(), outputs.get(i)) :
                    IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.filenameFromPath(inFile) + ".step") ;
            try (ByteArrayOutputStream os = CmdUtils.runExternal(String.format(cmdLine, inFile))) {
                IOUtils.ByteArrayOutputStreamToFile(os, outputFile);
            }
            pipe.addGeneratedFile(outputFile);
            outputFiles.add(outputFile);
            i++;
        }
        actualOutput = outputFiles;
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        return true;
    }
}
