package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.JSONUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

class JSONtoXMLStep extends Step {

    JSONtoXMLStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.JSON_XML;
    }

    @Override
    Object execAction(final List<String> inputFiles, final String... parameters) throws Exception {
        String rootElement = "JSON";
        String array = "array";
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1) {
                switch (parts[0].toLowerCase()) {
                    case "root":
                        rootElement = parts[1];
                        break;
                    case "array":
                        array = parts[1];
                        break;
                }
            }
        }

        List<String> providedOutputNames = listifyOutput(pipe);
        List<String> usedOutputFiles = new ArrayList<>();
        int i = 0;
        for (String inFile : inputFiles) {
            String xml = JSONUtils.JsonArrayToXmlString(IOUtils.loadStringFromFile(inFile), array, rootElement);
            String outputFile = providedOutputNames.size() > i && !providedOutputNames.get(i).equals("") ?
                    IOUtils.pathCombine(pipe.getWorkPath(), providedOutputNames.get(i)) :
                    IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.strippedFilename(inFile) + ".xml");
            IOUtils.saveStringToFile(xml, outputFile);
            usedOutputFiles.add(outputFile);
            i++;
        }

        pipe.addGeneratedFiles(usedOutputFiles);
        return usedOutputFiles;
    }

    @Override
    boolean assertParameter(final Parameter paramType, final Object param) {
        switch (paramType) {
            case INPUT:
                return assertStandardInput(param);
            case OUTPUT:
                return assertStandardOutput(output);
            case ADDITIONAL:
                return param == null || (param instanceof String && StringUtils.isEmpty((String) param));
            default:
                return true;
        }
    }
}
