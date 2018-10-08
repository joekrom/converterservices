package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

class MD5FilterStep extends Step {

    MD5FilterStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.MD5_FILTER;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        String extension = ".md5";
        String relativeMD5Path = "";
        boolean update = true;
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            String key = parts[0].toLowerCase();
            if (key.startsWith("ext") && parts.length > 1) {
                extension = parts[1];
            } else if ( (key.contains("path") || key.contains("dir") || key.contains("folder") )
                    && parts.length > 1) {
                relativeMD5Path = parts[1];
            } else if (key.equals("update") && parameter.toLowerCase().contains("false")) {
                update = false;
            }
        }


        List<String> outputFiles = new ArrayList<>();
        String md5File;
        // todo: create md5path if not present
        for (String inFile : inputFiles) {
            if (!IOUtils.isDirectory(inFile)) {
                md5File = (relativeMD5Path.equals("") ? inFile :
                            IOUtils.pathCombine(
                                    IOUtils.pathCombine(IOUtils.dirFromPath(inFile), relativeMD5Path),
                                    IOUtils.filenameFromPath(inFile)) )
                        + extension;
                String md5result = IOUtils.getMD5Checksum(inFile);
                if (!IOUtils.pathExists(md5File) || !IOUtils.loadStringFromFile(md5File).equals(md5result) ) {
                    if (update) {
                        IOUtils.saveStringToFile(md5result, md5File);
                        pipe.addGeneratedFile(md5File);
                    }
                    outputFiles.add(inFile);
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
