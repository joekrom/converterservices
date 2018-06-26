package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.CmdUtils;
import de.axxepta.converterservices.utils.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class CmdStep extends Step {

    CmdStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.CMD;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
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
                List<String> lines = CmdUtils.exec(String.format(cmdLine, ("\"" + inFile + "\"") ));
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
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        switch (paramType) {
            case INPUT:
                return assertStandardInput(param);
            case OUTPUT:
                return assertStandardOutput(param);
            case PARAMS:
                return ((String[]) param).length > 0;
        }
        return true;
    }
}
