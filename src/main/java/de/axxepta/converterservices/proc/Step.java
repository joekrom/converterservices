package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.tools.ZIPUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Step {

    private Type type;
    private Object input;
    private Object output;
    private Object additional;
    private Object params;

    Step(Type type, Object input, Object output, Object additional, Object params) {
        this.type = type;
        this.input = input;
        this.output = output;
        this.additional = additional;
        this.params = params;
    }

    Type getType() {
        return type;
    }

    Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    Object getAdditional() {
        return additional;
    }

    Object getParams() {
        return params;
    }

    Object exec(Pipeline.PipeRun pipe) throws Exception {
        if (StringUtils.isEmpty(input) && !(pipe.getLastOutput() instanceof List))
            throw new IllegalStateException("Last process step has wrong type!");

        List<String> inputFiles;
        if (StringUtils.isEmpty(input)) {
            inputFiles = (List) pipe.getLastOutput();
        } else {
            inputFiles = new ArrayList<>();
            if (input instanceof String) {
                if (type.equals(Type.XQUERY) && input.equals(Saxon.XQUERY_NO_CONTEXT)) {
                    inputFiles.add((String) input);
                } else {
                    inputFiles.add(pipedPath(input, pipe));
                }
            } else {
                for (Object inFile : (List) input) {
                    inputFiles.add(pipedPath(inFile, pipe));
                }
            }
        }

        Object outputObject = null;
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
        if (params instanceof Pipeline.SubPipeline) {
            Pipeline.SubPipeline paramsSub = (Pipeline.SubPipeline) params;
            pipe.execSubPipe(paramsSub);
            parameters = paramsSub.getOutput();
        } else {
            parameters = params;
        }

        String outputFile;
        switch (type) {
            case XSLT:
                String inputFile = inputFiles.get(0);
                outputFile = pipe.getWorkPath() + (StringUtils.isEmpty(output) ?
                        Saxon.standardOutputFilename((String) additionalInput) : (String) output);
                pipe.saxonTransform(inputFile, pipe.getInputPath() + additionalInput,
                        outputFile, parameters instanceof String ? (String) parameters : "");
                pipe.logFileAddArray(pipe.getErrFileArray());
                pipe.finalLogFileAdd(inputFile + ": " + pipe.getErrFileArray().getSize() + " messages");
                pipe.incErrorCounter(pipe.getErrFileArray().getSize());
                pipe.addLogSectionXsl(inputFile, pipe.getErrFileArray());
                pipe.addGeneratedFile(outputFile);
                outputObject = Arrays.asList(outputFile);
                break;
            case ZIP:
                outputFile = pipe.getWorkPath() + (StringUtils.isEmpty(output) ? "step" + pipe.getCounter() + ".zip" : output);
                List<String> additionalInputs = new ArrayList<>();
                if ((additionalInput instanceof List) && ((List) additionalInput).get(0) instanceof String) {
                    inputFiles.addAll((List<String>) additionalInput);
                } else if (additionalInput instanceof String) {
                    additionalInputs.add((String) additionalInput);
                }
                try {
                    ZIPUtils.zipRenamedFiles(outputFile, inputFiles, additionalInputs);
                } catch (IOException ex) {
                    pipe.finalLogFileAdd(String.format("--- Exception zipping files in step %s: %s", pipe.getCounter(), ex.getMessage()));
                }
                pipe.addGeneratedFile(outputFile);
                outputObject = Arrays.asList(outputFile);
                break;
            case XQUERY:
                String queryFile = pipedPath(additionalInput, pipe);
                String query = IOUtils.readTextFile(queryFile);
                Object queryOutput = pipe.saxonXQuery(query, inputFiles.get(0), (String) params);
                if (queryOutput instanceof Document) {
                    outputFile = StringUtils.isEmpty(output) ? "output_step" + pipe.getCounter() + ".xml" : (String) output;
                    Saxon.saveDOM((Document) queryOutput, outputFile);
                    pipe.addGeneratedFile(outputFile);
                    outputObject = outputFile;
                } else {
                    // ToDo: check cases, assure correct feeding in pipe
                    outputObject = queryOutput;
                }
                break;
            case XSL_FO:
                break;
            case UNZIP:
                break;
            case PDF_SPLIT:
                break;
            case PDF_MERGE:
                break;
            case THUMB:
                break;
            case EXIF:
                outputObject = "";
                break;
            case COMBINE:
                if (additionalInput instanceof String) {
                    inputFiles.add(pipedPath(additionalInput, pipe));
                } else {
                    for (Object inFile : (List) additionalInput) {
                        inputFiles.add(pipedPath(inFile, pipe));
                    }
                }
                outputObject = inputFiles;
                break;
            default:
        }

        return outputObject;
    }

    protected static String pipedPath(Object fileName, Pipeline.PipeRun pipe) throws IllegalArgumentException {
        if (!(fileName instanceof String)) {
            throw new IllegalArgumentException("Object is not a String");
        }
        return ((String) fileName).startsWith("pipe:") ? pipe.getWorkPath() + ((String) fileName).substring(5) :
                pipe.getInputPath() + fileName;
    }

    static boolean assertParameter(Type type, Parameter paramType, Object param) {
        if (StringUtils.isEmpty(param) &&
                !(paramType.equals(Parameter.ADDITIONAL) &&
                        (type.equals(Type.XSLT) || type.equals(Type.XQUERY) || type.equals(Type.COMBINE) ) ) )
            return true;
        if (paramType.equals(Parameter.PARAMS) && type.equals(Type.PDF_SPLIT) && !(param instanceof Boolean))
            return false;
        if (paramType.equals(Parameter.ADDITIONAL) && (type.equals(Type.ZIP) || type.equals(Type.COMBINE))
                && (param instanceof Pipeline.SubPipeline))
            return true;
        return (param instanceof String) || ((param instanceof List) && ((List) param).get(0) instanceof String);
    }

    public enum Type {
        XSLT, XSL_FO, XQUERY, ZIP, UNZIP, PDF_SPLIT, PDF_MERGE, THUMB, EXIF, COMBINE, NONE
    }

    enum Parameter {
        INPUT, OUTPUT, ADDITIONAL, PARAMS
    }
}
