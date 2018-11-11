package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

abstract class Step {

    private String name;
    Object input;
    Object output;
    Object additional;
    boolean stopOnError;
    String[] params;
    private Object actualOutput;

    Step(String name,  final Object input, final Object output, final Object additional, final boolean stopOnError, final String... params) {
        this.name = name;
        this.input = input;
        this.output = output;
        this.additional = additional;
        this.stopOnError = stopOnError;
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

    Object getActualOutput() {
        return actualOutput;
    }

    Object exec(final Pipeline pipe) throws Exception {
        if (StringUtils.isNoStringOrEmpty(input) && !(input instanceof Integer) && !(pipe.getLastOutput() instanceof List))
            throw new IllegalStateException("Last process step has wrong type!");

        List<String> inputFiles = resolveInput(input, pipe);

        String[] parameters;

        parameters = params;
        pipe.log("## at " + new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()));
        pipe.log("##   Input              : " +
                (getType().equals(Pipeline.StepType.HTTP_GET) || getType().equals(Pipeline.StepType.FTP_DOWN)?
                        input : inputFiles) );
        pipe.log("##   Additional Input   : " + additional);
        pipe.log("##   Parameters         : " + String.join(" - ", parameters));

        actualOutput = execAction(pipe, inputFiles, parameters);
        pipe.log("##   Output             : " + actualOutput);
        pipe.log("");
        return actualOutput;
    }

    abstract Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception;

    private static List outputList(Object oldOutput) {
        if (!(oldOutput instanceof List) ||
                (((List) oldOutput).size() > 0 && !(((List) oldOutput).get(0) instanceof String)) )
            throw new IllegalStateException("Referenced process step has wrong type!");
        return (List) oldOutput;
    }

    private static List<String> resolveInput(Object in, Pipeline pipe) {
        List<String> inputFiles;
        if (in == null || (in instanceof String && StringUtils.isEmpty((String) in))  ) {
            inputFiles = outputList( pipe.getLastOutput() );
        } else {
            inputFiles = resolveNotEmptyInput(in, pipe);
        }
        return inputFiles;
    }

    // use if input object is not empty String or null (asserted or otherwise)
    static List<String> resolveNotEmptyInput(final Object in, final Pipeline pipe) {
        List<String> inputFiles;
        if (in instanceof Integer) {
            Object oldOutput = pipe.getStepOutput((Integer) in);
            inputFiles = outputList(oldOutput);
        } else if (in instanceof String && ((String) in).startsWith(Pipeline.NAMED_STEP)) {
            inputFiles = outputList( pipe.getStepOutput(((String) in).substring(Pipeline.NAMED_STEP.length())) );
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
        return fileName.startsWith(Pipeline.FILE_INPUT) ?
                fileName.substring(Pipeline.FILE_INPUT.length()) :
                (fileName.startsWith(Pipeline.WORK_DIR) ?
                    IOUtils.pathCombine(pipe.getWorkPath(), fileName.substring(Pipeline.WORK_DIR.length())) :
                    IOUtils.pathCombine(pipe.getInputPath(), fileName.equals(".") ? "" : fileName) );
    }

    // transform output parameter to List of Strings, if List provided, none-String elements will be converted to empty String
    List<String> listifyOutput(Pipeline pipe) {
        List<String> outputFileNames = new ArrayList<>();
        if (output instanceof String && !StringUtils.isEmpty((String) output)) {
            outputFileNames.add((String) output);
        } else if (output instanceof List) {
            List outputList = (List) output;
            int i = 0;
            for (Object outputItem : outputList) {
                try {
                    outputFileNames.add((String) outputItem);
                } catch (ClassCastException ex) {
                    pipe.log(String.format("Output parameter %s in step %s is not a String, use standard output name!",
                            i, pipe.getCounter()));
                    outputFileNames.add("");
                }
                i++;
            }
        }
        return outputFileNames;
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

    static boolean assertNotEmptyInput(final Object param) {
        return param instanceof Integer || (param instanceof String && !StringUtils.isEmpty((String) param)) ||
                (param instanceof List && ((List) param).size() > 0);
    }

    static boolean assertEmptyInput(final Object param) {
        return param == null || (param instanceof String && StringUtils.isEmpty((String) param)) ||
                (param instanceof List && ((List) param).size() == 0);
    }

    abstract boolean assertParameter(final Parameter paramType, final Object param);

    enum Parameter {
        INPUT, OUTPUT, ADDITIONAL, PARAMS
    }
}
