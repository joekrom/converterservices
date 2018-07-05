package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class FilterStep extends Step {

    FilterStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.FILTER;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        List<String> outputFiles = new ArrayList<>();
        List<String> extension = new ArrayList<>();
        boolean recursive = false;
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts[0].toLowerCase().startsWith("ext") || parts[0].toLowerCase().equals("type") && parts.length > 1) {
                extension.add(parts[1].toLowerCase());
            } else if (parts[0].toLowerCase().startsWith("rec") && parts.length > 1 && parts[1].toLowerCase().equals("true")) {
                recursive = true;
            }
        }
        if (extension.size() < 1) {
            throw new IllegalArgumentException("Parameter EXT expected in filter step.");
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

    private static void addSubDirFiles(final List<String> output, final String dir, final String basisPath,
                                       final List<String> extensions, final boolean recursive)
            throws IOException
    {
        File directory = new File(dir);
        File[] filesList = directory.listFiles();
        if (filesList != null) {
            for (File file : filesList) {
                if (file.isFile() &&
                        (extensions.stream().anyMatch(e -> IOUtils.getFileExtension(file.getName()).toLowerCase().equals(e)))) {
                    output.add(file.getCanonicalPath());
                } else if (recursive) {
                    addSubDirFiles(output, file.getCanonicalPath(), basisPath, extensions, true);
                }
            }
        }
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        switch (paramType) {
            case INPUT:
                return ((param instanceof String) && !StringUtils.isNoStringOrEmpty(param) ) || (param instanceof Integer) ||
                        (param instanceof List && ((List) param).get(0) instanceof String);
            case PARAMS:
                return ((String[]) param).length > 0;
            default: return true;
        }
    }
}
