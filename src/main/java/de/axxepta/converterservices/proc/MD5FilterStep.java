package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

class MD5FilterStep extends Step {

    MD5FilterStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.MD5_FILTER;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        List<String> outputFiles = new ArrayList<>();
        String md5File;
        for (String inFile : inputFiles) {
            if (!IOUtils.isDirectory(inFile)) {
                md5File = inFile + ".md5";
                String md5result = IOUtils.getMD5Checksum(inFile);
                if (!IOUtils.pathExists(md5File) || IOUtils.loadStringFromFile(md5File).equals(md5result) ) {
                    IOUtils.saveStringToFile(md5result, md5File);
                    pipe.addGeneratedFile(md5File);
                    outputFiles.add(inFile);
                }
            }
        }
        actualOutput = outputFiles;
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        return true;
    }
}
