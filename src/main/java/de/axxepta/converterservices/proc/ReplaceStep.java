package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

class ReplaceStep extends Step {

    ReplaceStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.REPLACE;
    }

    @Override
    Object execAction(Pipeline pipe, List<String> inputFiles, String... parameters) throws Exception {
        List<String> replace = new ArrayList<>();
        List<String> with = new ArrayList<>();
        String fileReplace = "";    // don't use replace if not set as parameter
        String fileWith = "";
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1) {
                String regExp = parameter.substring(parameter.indexOf("=") + 1);
                switch (parts[0].toLowerCase()) {
                    case "replace":
                        replace.add(regExp);
                        break;
                    case "with":
                        with.add(regExp);
                        break;
                    case "filereplace":
                        fileReplace = regExp;
                        break;
                    case "filewith":
                        fileWith = regExp;
                        break;
                }
            }
        }
        int nReplaceDefs = Math.min(replace.size(), with.size());
        if (replace.size() != with.size()) {
            pipe.log("Unequal number of REPLACE/WITH parameters in replacement step " + pipe.getCounter());
        }

        List<String> outputNames = getOutputNames(inputFiles, fileReplace, fileWith, pipe);

        for (int i = 0; i < inputFiles.size(); i++) {
            String inFile = inputFiles.get(i);
            String text = IOUtils.readTextFile(inFile);
            for (int r = 0; r < nReplaceDefs; r++) {
                text = text.replace(replace.get(r), with.get(r));
            }
            IOUtils.saveStringToFile(text, outputNames.get(i));
            pipe.addGeneratedFile(outputNames.get(i));
        }

        actualOutput = outputNames;
        return outputNames;
    }

    private List<String> getOutputNames(final List<String> inputFiles, final String fileReplace, final String fileWith,
                                        final Pipeline pipe)
    {
        List<String> outputNames = new ArrayList<>();
        if (!fileReplace.equals("")) {
            for (String inFile: inputFiles) {
                outputNames.add(IOUtils.pathCombine(pipe.getWorkPath(),
                        IOUtils.filenameFromPath(inFile).replaceAll(fileReplace, fileWith)));
            }
        } else if (inputFiles.size() == 1 && output instanceof String) {
            outputNames.add(IOUtils.pathCombine(pipe.getWorkPath(), (String) output));
        } else if (output instanceof List && ((List) output).size() == inputFiles.size()) {
            for (Object outFile : (List)output) {
                outputNames.add(IOUtils.pathCombine(pipe.getWorkPath(), (String) outFile));
            }
        } else {
            for (String inFile : inputFiles) {
                outputNames.add(IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.filenameFromPath(inFile)));
            }
        }
        return outputNames;
    }

    @Override
    boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
