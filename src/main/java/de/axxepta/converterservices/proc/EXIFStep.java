package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.CmdUtils;
import de.axxepta.converterservices.utils.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class EXIFStep extends Step {

    EXIFStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.EXIF;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        boolean compact = false;
        boolean inPlace = false;
        String add = "";
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1) {
                switch (parts[0].toLowerCase()) {
                    case "short":
                        if (parts[1].toLowerCase().equals("true")) {
                            compact = true;
                        }
                    case "comp":
                        if (parts[1].toLowerCase().equals("false")) {
                            compact = true;
                        }
                        break;
                    case "inplace":
                        if (parts[1].toLowerCase().equals("true")) {
                            inPlace = true;
                        }
                        break;
                    case "add":
                        add = parameter.substring(parameter.indexOf("=") + 1);
                        break;
                }
                if (parameter.contains("short") && parameter.contains("true") ||
                        parameter.contains("comp") && parameter.contains("false")) {
                    compact = true;
                }
            }
        }

        List<String> outputFiles = new ArrayList<>();
        int i = 0;
        for (String inFile : inputFiles) {
            int outSize = 0;
            List<String> outputs;
            try {
                outputs = (List) output;
                outSize = outputs.size();
            } catch (Exception cc) {
                outputs = new ArrayList<>();
            }
            String outputFile = (inputFiles.size() == outSize) ?
                    IOUtils.pathCombine( (inPlace ? IOUtils.dirFromPath(inFile) : pipe.getWorkPath()), outputs.get(i)) :
                    IOUtils.pathCombine( (inPlace ? IOUtils.dirFromPath(inFile) : pipe.getWorkPath()),
                            IOUtils.filenameFromPath(inFile) + ".rdf") ;
            try {
                List<String> lines = CmdUtils.exifPipe(compact, "-X " + add, inFile);
                IOUtils.saveStringArrayToFile(lines.size() > 1 ? lines.subList(1, lines.size()) : lines,
                        outputFile, true);
            } catch (IOException ex) {
                pipe.log(String.format("Error executing external command %s:\n %s", "exif -X", ex.getMessage()));
            }
            pipe.addGeneratedFile(outputFile);
            outputFiles.add(outputFile);
            i++;
        }
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return !paramType.equals(Parameter.INPUT) || assertStandardInput(param);
    }

}
