package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class Step {

    protected Object input;
    protected Object output;
    protected Object additional;
    protected String[] params;
    protected Object actualOutput;

    Step(final Object input, final Object output, final Object additional, final String... params) {
        this.input = input;
        this.output = output;
        this.additional = additional;
        this.params = params;
    }

    abstract Pipeline.StepType getType();

    Object getInput() {
        return input;
    }

    void setInput(final Object input) {
        this.input = input;
    }

    public Object getActualOutput() {
        return actualOutput;
    }

    Object exec(final Pipeline pipe) throws Exception {
        if (StringUtils.isEmpty(input) && !(input instanceof Integer) && !(pipe.getLastOutput() instanceof List))
            throw new IllegalStateException("Last process step has wrong type!");

        List<String> inputFiles;
        if (StringUtils.isEmpty(input)) {
            inputFiles = (List) pipe.getLastOutput();
        } else if (input instanceof Integer) {
            Object oldOutput = pipe.getStepOutput((Integer) input);
            if (!(oldOutput instanceof List) || !(((List) oldOutput).get(0) instanceof String))
                throw new IllegalStateException("Last process step has wrong type!");
            inputFiles = (List) pipe.getStepOutput((Integer) input);
        } else {
            inputFiles = new ArrayList<>();
            if (input instanceof String) {
                inputFiles.add(pipedPath(input, pipe));
            } else {
                for (Object inFile : (List) input) {
                    inputFiles.add(pipedPath(inFile, pipe));
                }
            }
        }

        Object additionalInput;
        String[] parameters;
        // execute possible sub pipes first
        if (additional instanceof Pipeline.SubPipeline) {
            Pipeline.SubPipeline optionalSub = (Pipeline.SubPipeline) additional;
            pipe.execSubPipe(optionalSub);
            additionalInput = optionalSub.getOutput();
        } else {
            additionalInput = additional;
        }
        parameters = params;
        pipe.log("##   Input              : " + inputFiles);
        pipe.log("##   Additional Input   : " + additionalInput);
        pipe.log("##   Parameters         : " + String.join(" - ", parameters));

        Object outputObject = execAction(pipe, inputFiles, additionalInput, parameters);
        pipe.log("##   Output             : " + outputObject);
        pipe.log("");
        return outputObject;
    }

    abstract Object execAction(final Pipeline pipe, final List<String> inputFiles, final Object additionalInput,
                               final String... parameters) throws Exception;

    static String pipedPath(final Object fileNameObject, final Pipeline pipe) throws IllegalArgumentException {
        if (!(fileNameObject instanceof String)) {
            throw new IllegalArgumentException("Object is not a String");
        }
        String fileName = (String) fileNameObject;
        return (fileName.startsWith("pipe:") ?
                IOUtils.pathCombine(pipe.getWorkPath(), fileName.substring(5)) :
                IOUtils.pathCombine(pipe.getInputPath(), fileName.equals(".") ? "" : fileName));
    }

    static List<String> singleFileList(final String file) {
        List<String> outputFiles = new ArrayList<>();
        outputFiles.add(file);
        return outputFiles;
    }

    void assertParameters(final int step) throws IllegalArgumentException {
        if (!assertParameter(Step.Parameter.INPUT, input))
            throw new IllegalArgumentException(String.format("Wrong input type %s in step %s!", input.getClass(), step));
        if (!assertParameter(Step.Parameter.OUTPUT, output))
            throw new IllegalArgumentException(String.format("Wrong output type %s in step %s!", output.getClass(), step));
        if (!assertParameter(Step.Parameter.ADDITIONAL, additional))
            throw new IllegalArgumentException(String.format("Wrong additional input type %s in step %s!", additional.getClass(), step));
        if (!assertParameter(Step.Parameter.PARAMS, params))
            throw new IllegalArgumentException(String.format("Wrong process parameter type %s in step %s!", params.getClass(), step));
    }

    protected static boolean assertStandardInput(final Object param) {
        return assertStandardOutput(param) || param instanceof Integer;
    }

    protected static boolean assertStandardOutput(final Object param) {
        return param == null || param instanceof String ||
                (param instanceof List && ((List) param).get(0) instanceof String);
    }

    protected abstract boolean assertParameter(final Parameter paramType, final Object param);

    enum Parameter {
        INPUT, OUTPUT, ADDITIONAL, PARAMS
    }
}
