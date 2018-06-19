package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class ListStep extends Step {

    ListStep(Object input, Object output, Object additional, String... params) {
        super(input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.LIST;
    }

    @Override
    Object execAction(Pipeline pipe, List<String> inputFiles, Object additionalInput, String... parameters) throws Exception {
        boolean xml = false;
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts[0].toLowerCase().startsWith("xml") && parts.length > 1 && parts[1].toLowerCase().equals("true")) {
                xml = true;
            }
        }
        String outputFile = IOUtils.pathCombine(pipe.getWorkPath(),
                StringUtils.isEmpty(output) ? "list_step" + pipe.getCounter() + (xml ? ".xml" : ".txt") : (String) output);

        if (xml) {
            List<String> xmlLines = new ArrayList<>();
            xmlLines.add("<list>");
            xmlLines.addAll(inputFiles.stream().map(s -> "  <item>" + s + "</item>").collect(Collectors.toList()));
            xmlLines.add("</list>");
            IOUtils.saveStringArrayToFile(xmlLines, outputFile, true);
        } else {
            IOUtils.saveStringArrayToFile(inputFiles, outputFile, true);
        }

        pipe.addGeneratedFile(outputFile);
        actualOutput = outputFile;
        return singleFileList(outputFile);
    }

    @Override
    protected boolean assertParameter(Parameter paramType, Object param) {
        return !paramType.equals(Parameter.INPUT) || assertStandardInput(param);
    }
}
