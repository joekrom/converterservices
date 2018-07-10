package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ZIPUtils;
import de.axxepta.converterservices.utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

class GUnzipStep extends Step {

    GUnzipStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.GUNZIP;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        // type gzip (standard) or zLib--zLib has additional headers and is used by PHP's gzcompress function
        // corresponds to Java's GZIPInputStream and InflaterInputStream classes
        String type = "gzip";
        for (String line : parameters) {
            String[] parts = line.split(" *= *");
            if (parts.length > 1 && parts[0].toLowerCase().startsWith("type") || parts[0].toLowerCase().startsWith("lib")) {
                type = parts[1].toLowerCase();
            }
        }

        List<String> providedOutputNames = listifyOutput(pipe);
        List<String> usedOutputFiles = new ArrayList<>();

        int i = 0;
        for (String inFile : inputFiles) {
            String outputFile = getCurrentOutputFile(providedOutputNames, i, pipe);
            byte[] zippedArray = IOUtils.loadByteArrayFromFile(inFile);
            byte[] unzipped = type.equals("zlib") ?
                    ZIPUtils.zUnzipByteArray(zippedArray) :
                    ZIPUtils.gUnzipByteArray(zippedArray);
            IOUtils.byteArrayToFile(unzipped, outputFile);
            pipe.addGeneratedFile(outputFile);
            usedOutputFiles.add(outputFile);
            i++;
        }

        actualOutput = usedOutputFiles;
        return usedOutputFiles;
    }

    private String getCurrentOutputFile(final List<String> providedOutputNames, final int current, final Pipeline pipe) {
        return providedOutputNames.size() > current && !providedOutputNames.get(current).equals("") ?
                IOUtils.pathCombine(pipe.getWorkPath(), providedOutputNames.get(current)) :
                IOUtils.pathCombine(pipe.getWorkPath(),"step_" + pipe.getCounter() + ".unzip");
    }

    @Override
    boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }
}
