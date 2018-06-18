package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.CmdUtils;
import de.axxepta.converterservices.utils.IOUtils;

import java.io.IOException;
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
        String cmdLine = parameters[0];
        List<String> outputFiles = new ArrayList<>();
        int i = 0;
        for (String inFile : inputFiles) {
            int outSize = 0;
            List<String> outputs = new ArrayList<>();
            try {
                outputs = (List) output;
                outSize = outputs.size();
            } catch (Exception cc) {}
            String outputFile = (inputFiles.size() == outSize) ?
                    IOUtils.pathCombine(pipe.getWorkPath(), outputs.get(i)) :
                    IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.filenameFromPath(inFile) + ".step") ;
            try {
                List<String> lines = CmdUtils.exec(String.format(cmdLine, inFile));
                IOUtils.saveStringArrayToFile(lines.size() > 1 ? lines.subList(1, lines.size() - 1) : lines,
                        outputFile, false);
            } catch (IOException ex) {
                pipe.log(String.format("Error executing external command %s:\n %s", cmdLine, ex.getMessage()));
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
