package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ZIPUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

class UnzipStep extends Step {

    UnzipStep(String name, Object input, Object output, Object additional, boolean stopOnError, String... params) {
        super(name, input, output, additional, stopOnError, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.UNZIP;
    }

    @Override
    Object execAction(final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        List<String> outputFiles = new ArrayList<>();
        if ((additional instanceof String) && (parameters.length > 0 && parameters[0].toLowerCase().contains("true"))) {
            String outputFile = (output instanceof String) ? (String) output : (String) additional;
            ZIPUtils.unzipSingle(inputFiles.get(0), (String) additional, pipe.getWorkPath(), outputFile);
            outputFiles.add(outputFile);
        } else {
            outputFiles.addAll(ZIPUtils.unzip(inputFiles.get(0), pipe.getWorkPath()));
        }
        pipe.addGeneratedFiles(outputFiles);
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        if (StringUtils.isNoStringOrEmpty(param))
            return true;
        if (paramType.equals(Parameter.PARAMS)) {
            return param instanceof Boolean;
        }
        return (param instanceof String);
    }
}
