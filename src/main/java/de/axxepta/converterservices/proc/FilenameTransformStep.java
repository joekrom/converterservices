package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

class FilenameTransformStep extends Step {

    FilenameTransformStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.FT;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        String ext = null;
        String extPattern = "";
        String fileBase = null;
        String filePattern = "";
        String dir = null;
        String dirPattern = "";
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1) {
                String pattern = parameter.substring(parameter.indexOf("=") + 1);
                switch (parts[0].toLowerCase()) {
                    case "ext": case "extension":
                        ext = pattern;
                        break;
                    case "extpattern": case "extensionpattern":
                        extPattern = pattern;
                        break;
                    case "filebase": case "file":
                        fileBase = pattern;
                        break;
                    case "filebasepattern": case "filepattern":
                        filePattern = pattern;
                        break;
                    case "dir": case "directory": case "path":
                        dir = pattern;
                        break;
                    case "dirpattern": case "directorypattern": case "pathpattern":
                        dirPattern = pattern;
                        break;
                }
            }
        }

        List<String> outputNames = new ArrayList<>();
        for (String inFile : inputFiles) {
            String fileDir = IOUtils.dirFromPath(inFile);
            String fileName = IOUtils.strippedFilename(inFile);
            String fileExt = IOUtils.getFileExtension(inFile);
            if (dir != null) {
                if (dirPattern.equals("")) {
                    fileDir = dir;
                } else {
                    fileDir = fileDir.replaceAll(dirPattern, dir);
                }
            }
            if (fileBase != null) {
                if (filePattern.equals("")) {
                    fileName = fileBase;
                } else {
                    fileName = fileName.replaceAll(filePattern, fileBase);
                }
            }
            if (ext != null) {
                if (extPattern.equals("")) {
                    fileExt = ext;
                } else {
                    fileExt = fileExt.replaceAll(extPattern, ext);
                }
            }
            outputNames.add(IOUtils.pathCombine(fileDir, fileName + (fileExt.equals("") ? "" : ".") + fileExt));
        }

        return outputNames;
    }

    @Override
    boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
