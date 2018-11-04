package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

class MD5Step extends Step {

    MD5Step(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.MD5;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        String extension = ".md5";
        String relativeMD5Path = "";
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            String key = parts[0].toLowerCase();
            if (key.startsWith("ext") && parts.length > 1) {
                extension = parts[1];
            } else if ( (key.contains("path") || key.contains("dir") || key.contains("folder") )
                    && parts.length > 1) {
                relativeMD5Path = parts[1];
            }
        }

        List<String> outputFiles = new ArrayList<>();
        String outputFile;
        for (String inFile : inputFiles) {
            if (!IOUtils.isDirectory(inFile)) {
                try {
                    outputFile = (relativeMD5Path.equals("") ? inFile :
                            IOUtils.pathCombine(
                                    IOUtils.pathCombine(IOUtils.dirFromPath(inFile), relativeMD5Path),
                                    IOUtils.filenameFromPath(inFile)))
                            + extension;

                    String md5result = IOUtils.getMD5Checksum(inFile);
                    IOUtils.saveStringToFile(md5result, outputFile);
                    pipe.addGeneratedFile(outputFile);
                } catch (Exception ex) {
                    pipe.log("Error during md5 step, input file " + inFile + ": " + ex.getMessage());
                }
            }
        }

        return outputFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return !paramType.equals(Parameter.INPUT)|| assertStandardInput(param);
    }
}
