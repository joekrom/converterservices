package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

class Base64DecodeStep extends Step {

    Base64DecodeStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.BASE64_ENC;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        List<String> providedOutputNames = listifyOutput(pipe);
        List<String> usedOutputFiles = new ArrayList<>();

        int i = 0;
        for (String inFile : inputFiles) {
            String outputFile = getCurrentOutputFile(providedOutputNames, i, pipe);
            String text = IOUtils.loadStringFromFile(inFile);
            byte[] decoded = Base64.getDecoder().decode(text);
            IOUtils.byteArrayToFile(decoded, outputFile);
            pipe.addGeneratedFile(outputFile);
            usedOutputFiles.add(outputFile);
            i++;
        }

        return usedOutputFiles;
    }

    private String getCurrentOutputFile(final List<String> providedOutputNames, final int current, final Pipeline pipe) {
        return providedOutputNames.size() > current && !providedOutputNames.get(current).equals("") ?
                IOUtils.pathCombine(pipe.getWorkPath(), providedOutputNames.get(current)) :
                IOUtils.pathCombine(pipe.getWorkPath(),"step_" + pipe.getCounter() + "_" + current + ".b64");
    }

    @Override
    boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
