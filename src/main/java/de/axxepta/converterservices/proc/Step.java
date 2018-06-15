package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class Step {

    protected Object input;
    protected Object output;
    protected Object additional;
    protected Object[] params;
    protected Object actualOutput;

    Step(Object input, Object output, Object additional, Object... params) {
        this.input = input;
        this.output = output;
        this.additional = additional;
        this.params = params;
    }

    abstract Pipeline.StepType getType();

    Object getInput() {
        return input;
    }

    void setInput(Object input) {
        this.input = input;
    }

    public Object getActualOutput() {
        return actualOutput;
    }

    Object exec(Pipeline pipe) throws Exception {
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
        Object parameters;
        // execute possible sub pipes first
        if (additional instanceof Pipeline.SubPipeline) {
            Pipeline.SubPipeline optionalSub = (Pipeline.SubPipeline) additional;
            pipe.execSubPipe(optionalSub);
            additionalInput = optionalSub.getOutput();
        } else {
            additionalInput = additional;
        }
        if (params[0] instanceof Pipeline.SubPipeline) {
            Pipeline.SubPipeline paramsSub = (Pipeline.SubPipeline) params[0];
            pipe.execSubPipe(paramsSub);
            parameters = paramsSub.getOutput();
        } else {
            parameters = params;
        }
        pipe.log("##   Input              : " + inputFiles);
        pipe.log("##   Additional Input   : " + additionalInput);
        pipe.log("##   Parameters         : " + parameters);

        Object outputObject = execAction(inputFiles, additionalInput, parameters, pipe);
        pipe.log("##   Output             : " + outputObject);
        pipe.log("");
        return outputObject;
    }

    abstract Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception;

    static String pipedPath(Object fileName, Pipeline pipe) throws IllegalArgumentException {
        if (!(fileName instanceof String)) {
            throw new IllegalArgumentException("Object is not a String");
        }
        return ((String) fileName).startsWith("pipe:") ?
                IOUtils.pathCombine(pipe.getWorkPath(), ((String) fileName).substring(5)) :
                IOUtils.pathCombine(pipe.getInputPath(), (String) fileName);
    }

    String getStandardOutputFile(int step, Pipeline pipe) {
        return IOUtils.pathCombine(pipe.getWorkPath(),
                StringUtils.isEmpty(output) ? "output_step_" + step + ".xml" : (String) output );
    }

    static List<String> singleFileList(String file) {
        List<String> outputFiles = new ArrayList<>();
        outputFiles.add(file);
        return outputFiles;
    }

    void assertParameters() throws IllegalArgumentException {
        if (!assertParameter(Step.Parameter.INPUT, input))
            throw new IllegalArgumentException("Wrong input type!");
        if (!assertParameter(Step.Parameter.OUTPUT, output))
            throw new IllegalArgumentException("Wrong output type!");
        if (!assertParameter(Step.Parameter.ADDITIONAL, additional))
            throw new IllegalArgumentException("Wrong additional input type!");
        if (!assertParameter(Step.Parameter.PARAMS, params))
            throw new IllegalArgumentException("Wrong process parameter type!");
    }

    protected abstract boolean assertParameter(Parameter paramType, Object param);

    enum Parameter {
        INPUT, OUTPUT, ADDITIONAL, PARAMS
    }
}
