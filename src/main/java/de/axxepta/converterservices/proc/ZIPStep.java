package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ZIPUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ZIPStep extends Step {

    ZIPStep(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.ZIP;
    }

    @Override
    Object execAction(Pipeline pipe, List<String> inputFiles, Object additionalInput, String... parameters) throws Exception {
        String outputFile = IOUtils.pathCombine(pipe.getWorkPath(),
                StringUtils.isEmpty(output) ? "step" + pipe.getCounter() + ".zip" : (String) output);
        List<String> additionalInputs = new ArrayList<>();
        if ((additionalInput instanceof List) && ((List) additionalInput).get(0) instanceof String) {
            inputFiles.addAll((List<String>) additionalInput);
        } else if (additionalInput instanceof String) {
            additionalInputs.add((String) additionalInput);
        } else {
            for (String inputFile : inputFiles) {
                additionalInputs.add(IOUtils.relativePath(inputFile, pipe.getWorkPath()));
            }
        }
        try {
            ZIPUtils.zipRenamedFiles(outputFile, inputFiles, additionalInputs);
        } catch (IOException ex) {
            pipe.finalLogFileAdd(String.format("--- Exception zipping files in step %s: %s", pipe.getCounter(), ex.getMessage()));
        }
        pipe.addGeneratedFile(outputFile);
        actualOutput = outputFile;
        return singleFileList(outputFile);
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        if (StringUtils.isEmpty(param))
            return true;
        return (paramType.equals(Parameter.ADDITIONAL) && (param instanceof Pipeline.SubPipeline)) ||
            assertStandardInput(param);
    }
}
