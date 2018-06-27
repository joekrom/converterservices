package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class Step {

    private String name;
    protected Object input;
    protected Object output;
    protected Object additional;
    protected String[] params;
    protected Object actualOutput;

    /**
     * "Protocol" identifier prefix for inputs to be taken from working directory
     */
    public static final String WORK_DIR = "pipe://";

    /**
     * "Protocol" identifier prefix for inputs to be taken from previously executed named step
     */
    public static final String NAMED_STEP = "step://";

    Step(String name,  final Object input, final Object output, final Object additional, final String... params) {
        this.name = name;
        this.input = input;
        this.output = output;
        this.additional = additional;
        this.params = params;
    }

    abstract Pipeline.StepType getType();

    String getName() {
        return name;
    }

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
        if (StringUtils.isNoStringOrEmpty(input) && !(input instanceof Integer) && !(pipe.getLastOutput() instanceof List))
            throw new IllegalStateException("Last process step has wrong type!");

        List<String> inputFiles = resolveInput(input, pipe);

        Object additionalInput;
        String[] parameters;

        parameters = params;
        pipe.log("##   Input              : " + inputFiles);
        pipe.log("##   Additional Input   : " + additional);
        pipe.log("##   Parameters         : " + String.join(" - ", parameters));

        Object outputObject = execAction(pipe, inputFiles, parameters);
        pipe.log("##   Output             : " + outputObject);
        pipe.log("");
        return outputObject;
    }

    abstract Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception;

    private static List outputList(Object oldOutput) {
        if (!(oldOutput instanceof List) ||
                (((List) oldOutput).size() > 0 && !(((List) oldOutput).get(0) instanceof String)) )
            throw new IllegalStateException("Referenced process step has wrong type!");
        return (List) oldOutput;
    }

    static List<String> resolveInput(Object in, Pipeline pipe) {
        List<String> inputFiles;
        if (in == null || (in instanceof String && StringUtils.isEmpty((String) in))  ) {
            inputFiles = outputList( pipe.getLastOutput() );
        } else if (in instanceof Integer) {
            Object oldOutput = pipe.getStepOutput((Integer) in);
            inputFiles = outputList(oldOutput);
        } else if (in instanceof String && ((String) in).startsWith(NAMED_STEP)) {
            inputFiles = outputList( pipe.getStepOutput(((String) in).substring(NAMED_STEP.length())) );
        } else {
            inputFiles = new ArrayList<>();
            if (in instanceof String) {
                inputFiles.add(pipedPath(in, pipe));
            } else {
                for (Object inFile : (List) in) {
                    inputFiles.add(pipedPath(inFile, pipe));
                }
            }
        }
        return inputFiles;
    }

    static String pipedPath(final Object fileNameObject, final Pipeline pipe) throws IllegalArgumentException {
        if (!(fileNameObject instanceof String)) {
            throw new IllegalArgumentException("Object is not a String");
        }
        String fileName = (String) fileNameObject;
        return fileName.startsWith(WORK_DIR) ?
                IOUtils.pathCombine(pipe.getWorkPath(), fileName.substring(WORK_DIR.length())) :
                IOUtils.pathCombine(pipe.getInputPath(), fileName.equals(".") ? "" : fileName);
    }

    static List<String> singleFileList(final String file) {
        final List<String> outputFiles = new ArrayList<>();
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

    static boolean assertStandardInput(final Object param) {
        return assertStandardOutput(param) || param instanceof Integer;
    }

    static boolean assertStandardOutput(final Object param) {
        return param == null || param instanceof String ||
                (param instanceof List && ((List) param).get(0) instanceof String);
    }

    abstract boolean assertParameter(final Parameter paramType, final Object param);

    enum Parameter {
        INPUT, OUTPUT, ADDITIONAL, PARAMS
    }
}
