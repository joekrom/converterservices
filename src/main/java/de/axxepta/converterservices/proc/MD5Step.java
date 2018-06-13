package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.CmdUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

class MD5Step extends Step {

    MD5Step(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.MD5;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        List<String> outputFiles = new ArrayList<>();
        String outputFile;
        for (String inFile : inputFiles) {
            //outputFile = StringUtils.isEmpty(output) ? inFile + ".md5" : (String) output;
            // check directory, check length of output and type match List/String

            outputFile = IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.filenameFromPath(inFile) + ".md5");
            List<String> md5result = CmdUtils.exec("md5sum " + inFile);
            IOUtils.saveStringArrayToFile(md5result, outputFile);
            pipe.addGeneratedFile(outputFile);
            outputFiles.add(outputFile);
        }
        return outputFiles;
    }

    @Override
    boolean assertParameter(Parameter paramType, Object param) {
        return true;
    }
}
