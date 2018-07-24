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
        if (parameters.length > 0) {
            String parameter = parameters[0].toLowerCase();
            if (parameter.contains("short") && parameter.contains("true") ||
                    parameter.contains("comp") && parameter.contains("false")) {
                compact = true;
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
                    IOUtils.pathCombine(pipe.getWorkPath(), outputs.get(i)) :
                    IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.filenameFromPath(inFile) + ".rdf") ;
            try {
                List<String> lines = CmdUtils.exifPipe(compact, "-X", inFile);
                IOUtils.saveStringArrayToFile(lines.size() > 1 ? lines.subList(1, lines.size() - 1) : lines,
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
