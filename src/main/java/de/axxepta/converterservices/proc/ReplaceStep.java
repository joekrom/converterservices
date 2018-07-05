package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ReplaceStep extends Step {

    private static Logger LOGGER = LoggerFactory.getLogger(ReplaceStep.class);

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
        String charset = "UTF-8";
        boolean inPlace = false;
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
                    case "charset": case "encoding":
                        charset = regExp;
                        break;
                    case "inplace":
                        if (regExp.toLowerCase().equals("true"))
                            inPlace = true;
                        break;
                }
            }
        }

        int nReplaceDefs = Math.min(replace.size(), with.size());
        if (replace.size() != with.size()) {
            pipe.log("Unequal number of REPLACE/WITH parameters in replacement step " + pipe.getCounter());
        }

        List<String> outputNames = getOutputNames(inputFiles, fileReplace, fileWith, inPlace, pipe);

        int i = 0;
        for (String inFile : inputFiles) {
            try {
                String text = IOUtils.loadStringFromFile(inFile, charset);
                for (int r = 0; r < nReplaceDefs; r++) {
                    text = text.replace(replace.get(r), with.get(r));
                }
                IOUtils.saveStringToFile(text, outputNames.get(i), charset);
                if (!inPlace) {
                    pipe.addGeneratedFile(outputNames.get(i));
                }
            } catch (IOException ex) {
                pipe.log("Text replacement or file operation failed at file " + inFile);
                LOGGER.warn("Text replacement or file operation failed at file " + inFile, ex);
            }
            i++;
        }

        actualOutput = outputNames;
        return outputNames;
    }

    private List<String> getOutputNames(final List<String> inputFiles, final String fileReplace, final String fileWith,
                                        final boolean inPlace, final Pipeline pipe)
    {
        List<String> outputNames = new ArrayList<>();
        if (!fileReplace.equals("")) {
            for (String inFile: inputFiles) {
                outputNames.add(
                        IOUtils.pathCombine(inPlace ? IOUtils.dirFromPath(inFile) : pipe.getWorkPath(),
                                IOUtils.filenameFromPath(inFile).replaceAll(fileReplace, fileWith))
                );
            }
        } else if (inputFiles.size() == 1 && output instanceof String) {
            outputNames.add(
                    IOUtils.pathCombine(inPlace ? IOUtils.dirFromPath(inputFiles.get(0)) : pipe.getWorkPath(),
                            (String) output)
            );
        } else if (output instanceof List && ((List) output).size() == inputFiles.size()) {
            int i = 0;
            for (Object outFile : (List)output) {
                outputNames.add(
                        IOUtils.pathCombine(inPlace ? IOUtils.dirFromPath(inputFiles.get(i)) : pipe.getWorkPath(),
                                (String) outFile)
                );
                i++;
            }
        } else {
            for (String inFile : inputFiles) {
                outputNames.add(inPlace ? inFile :
                        IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.filenameFromPath(inFile))
                );
            }
        }
        return outputNames;
    }

    @Override
    boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
