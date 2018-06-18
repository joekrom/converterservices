package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

class MD5Step extends Step {

    MD5Step(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.MD5;
    }

    @Override
    Object execAction(Pipeline pipe, List<String> inputFiles, Object additionalInput, String... parameters) throws Exception {
        List<String> outputFiles = new ArrayList<>();
        String outputFile;
        for (String inFile : inputFiles) {

            //outputFile = StringUtils.isEmpty(output) ? inFile + ".md5" : (String) output;
            // check directory, check length of output and type match List/String

            outputFile = IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.filenameFromPath(inFile) + ".md5");
            String md5result = IOUtils.getMD5Checksum(inFile);
            IOUtils.saveStringToFile(md5result, outputFile);
            pipe.addGeneratedFile(outputFile);
            outputFiles.add(outputFile);
        }
        actualOutput = outputFiles;
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        return !paramType.equals(Parameter.INPUT)|| assertStandardInput(param);
    }
}
