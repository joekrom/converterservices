package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

class MD5FilterStep extends Step {

    MD5FilterStep(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.MD5_FILTER;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final Object additionalInput, final String... parameters)
            throws Exception
    {
        String extension = ".md5";
        String relativeMD5Path = "";
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts[0].toLowerCase().startsWith("ext") && parts.length > 1) {
                extension = parts[1];
            } else if ( (parts[0].toLowerCase().contains("path") || parts[0].toLowerCase().contains("dir")
                            || parts[0].toLowerCase().contains("folder") )
                    && parts.length > 1) {
                relativeMD5Path = parts[1];
            }
        }


        List<String> outputFiles = new ArrayList<>();
        String md5File;
        for (String inFile : inputFiles) {
            if (!IOUtils.isDirectory(inFile)) {
                md5File = (relativeMD5Path.equals("") ? inFile :
                            IOUtils.pathCombine(
                                    IOUtils.pathCombine(IOUtils.dirFromPath(inFile), relativeMD5Path),
                                    IOUtils.filenameFromPath(inFile)) )
                        + extension;
                String md5result = IOUtils.getMD5Checksum(inFile);
                if (!IOUtils.pathExists(md5File) || !IOUtils.loadStringFromFile(md5File).equals(md5result) ) {
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
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return !paramType.equals(Parameter.INPUT)|| assertStandardInput(param);
    }
}
