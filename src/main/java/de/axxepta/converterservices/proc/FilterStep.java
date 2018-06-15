package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class FilterStep extends Step {

    FilterStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.FILTER;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        List<String> outputFiles = new ArrayList<>();
        List<String> extension = new ArrayList<>();
        boolean recursive = false;
        if (!(parameters instanceof String[])) {
            throw new IllegalArgumentException("Parameter EXT expected in filter step.");
        } else {
            for (String parameter : (String[])parameters) {
                String[] parts = parameter.split(" *= *");
                if (parts[0].toLowerCase().startsWith("ext") && parts.length > 1) {
                    extension.add(parts[1].toLowerCase());
                } else if (parts[0].toLowerCase().startsWith("rec")) {
                    recursive = true;
                }
            }
            if (extension.size() < 1) {
                throw new IllegalArgumentException("Parameter EXT expected in filter step.");
            }
        }
        for (String inFile : inputFiles) {
            if (IOUtils.pathExists(inFile)) {
                if (IOUtils.isDirectory(inFile)) {
                    addSubDirFiles(outputFiles, inFile, pipe.getInputPath(), extension, recursive);
                } else {
                    if (extension.stream().anyMatch(e -> IOUtils.getFileExtension(inFile).toLowerCase().equals(e))) {
                        outputFiles.add(inFile);
                    }
                }
            }
        }
        actualOutput = outputFiles;
        return outputFiles;
    }

    private static void addSubDirFiles(List<String> output, String dir, String basisPath, List<String> extensions, boolean recursive)
            throws IOException
    {
        File directory = new File(dir);
        File[] filesList = directory.listFiles();
        for (File file : filesList) {
            if (file.isFile() &&
                    (extensions.stream().anyMatch(e -> IOUtils.getFileExtension(file.getName()).toLowerCase().equals(e))) ) {
                output.add(file.getCanonicalPath());
            } else if (recursive) {
                addSubDirFiles(output, file.getCanonicalPath(), basisPath, extensions, true);
            }
        }
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        switch (paramType) {
            case INPUT:
                return ((param instanceof String) && StringUtils.isEmpty(param) ) || (param instanceof Integer) ||
                        (param instanceof List && ((List) param).get(0) instanceof String);
            case PARAMS:
                return param instanceof String[] && ((String[]) param).length > 0;
            default: return true;
        }
    }
}
