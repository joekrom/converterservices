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
    Pipeline pipe = null;

    Step(final String name, final Object input, final Object output, final Object additional, final boolean stopOnError, final String... params) {
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

        this.pipe = pipe;

        List<String> inputFiles = resolveInput(input,false);

        String[] parameters;

        parameters = params;
        pipe.log("## at " + new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()));
        pipe.log("##   Input              : " +
                (getType().equals(Pipeline.StepType.HTTP_GET) || getType().equals(Pipeline.StepType.FTP_DOWN)?
                        input : inputFiles) );
        pipe.log("##   Additional Input   : " + additional);
        pipe.log("##   Parameters         : " + String.join(" - ", parameters));

        actualOutput = execAction(inputFiles, parameters);
        pipe.log("##   Output             : " + actualOutput);
        pipe.log("");
        return actualOutput;
    }

    abstract Object execAction(final List<String> inputFiles, final String... parameters) throws Exception;

    private static List outputList(Object oldOutput) {
        if (!(oldOutput instanceof List) ||
                (((List) oldOutput).size() > 0 && !(((List) oldOutput).get(0) instanceof String)) )
            throw new IllegalStateException("Referenced process step has wrong type!");
        return (List) oldOutput;
    }

    List<String> resolveInput(final Object in, boolean additional) {
        List<String> inputFiles;
        if (in == null || (in instanceof String && StringUtils.isEmpty((String) in))  ) {
            inputFiles = additional ? new ArrayList<>() : outputList( pipe.getLastOutput() );
        } else {
            inputFiles = resolveNotEmptyInput(in);
        }
        return inputFiles;
    }

    // use if input object is not empty String or null (asserted or otherwise)
    List<String> resolveNotEmptyInput(final Object in) {
        List<String> inputFiles = new ArrayList<>();
        if (in instanceof Integer) {
            Object oldOutput = pipe.getStepOutput((Integer) in);
            inputFiles.addAll(outputList(oldOutput));
        } else if (in instanceof String) {
            if (((String) in).startsWith(Pipeline.NAMED_STEP)) {
                inputFiles.addAll(outputList(pipe.getStepOutput( ((String) in).substring(Pipeline.NAMED_STEP.length()))));
            } else {
                inputFiles.addAll(resolvePathExpr(pipedPath(in)));
            }
        } else if (in instanceof List) {
            for (Object inFile : (List) in) {
                if (inFile instanceof Integer) {
                    Object oldOutput = pipe.getStepOutput((Integer) inFile);
                    inputFiles.addAll(outputList(oldOutput));
                } else if (inFile instanceof String) {
                    if (((String) inFile).startsWith(Pipeline.NAMED_STEP)) {
                        inputFiles.addAll(outputList(pipe.getStepOutput( ((String) inFile).substring(Pipeline.NAMED_STEP.length()))));
                    } else {
                        inputFiles.addAll(resolvePathExpr(pipedPath(inFile)));
                    }
                }
            }
        }
        return inputFiles;
    }

    String pipedPath(final Object fileNameObject) throws IllegalArgumentException {
        if (!(fileNameObject instanceof String)) {
            throw new IllegalArgumentException("Object is not a String");
        }
        String fileName = (String) fileNameObject;
        return fileName.startsWith(Pipeline.FILE_INPUT) ?
                fileName.substring(Pipeline.FILE_INPUT.length()) :
                (fileName.startsWith(Pipeline.WORK_DIR) ?
                    IOUtils.pathCombine(pipe.getWorkPath(), fileName.substring(Pipeline.WORK_DIR.length())) :
                        (fileName.startsWith(Pipeline.REGEXP_INPUT) ?
                            fileName :
                            IOUtils.pathCombine(pipe.getInputPath(), fileName.equals(".") ? "" : fileName) ) );
    }

    private List<String> resolvePathExpr(String path) {
        List<String> files = new ArrayList<>();
        if (path.startsWith(Pipeline.REGEXP_INPUT)) {
            if (IOUtils.isDirectory(pipe.getInputPath())) {
                files.addAll( IOUtils.resolvePathRegexp(pipe.getInputPath(), path.substring(Pipeline.REGEXP_INPUT.length()), pipe::log) );
            }
        } else {
            if ((path.contains("*") || path.contains("?")) &&
                    !path.contains("="))    // don't resolve HTTP paths with parameters!
            {
                files.addAll(IOUtils.resolveBlobExpression(path, pipe::log));
            } else {
                files.add(path);
            }
        }
        return files;
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

    String standardOutputFile(final Pipeline pipe) {
        return standardOutputFile(pipe, "xml");
    }

    String standardOutputFile(final Pipeline pipe, String ext) {
        return IOUtils.pathCombine(pipe.getWorkPath(),
                StringUtils.isNoStringOrEmpty(output) ?
                "step_" + pipe.getCounter() + "." + ext :
                (String) output
        );
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
