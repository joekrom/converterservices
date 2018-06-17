package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.App;
import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.tools.ZIPUtils;
import de.axxepta.converterservices.utils.*;
import net.sf.saxon.s9api.SaxonApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Pipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pipeline.class);

    private boolean verbose;
    private boolean archive;
    private boolean cleanup;
    private String dateString;
    private String workPath;
    private String inputPath;
    private String outputPath;
    private String inputFile;
    private String logFileName;
    private String logLevel;
    private List<Step> steps;

    private int stepCounter = 0;
    private int errorCounter = 0;

    private Saxon saxon;
    private LoggingErrorListener err;
    private FileArray logFileFinal = new FileArray();
    private FileArray logFile = new FileArray();

    private List<String> generatedFiles = new ArrayList<>();
    private Object lastOutput = new ArrayList<>();

    private Pipeline(PipelineBuilder builder) {
        this.verbose = builder.verbose;
        this.archive = builder.archive;
        this.cleanup = builder.cleanup;
        this.dateString = builder.dateString;
        this.workPath = builder.workPath;
        this.inputPath = builder.inputPath;
        this.outputPath = builder.outputPath;
        this.inputFile = builder.inputFile;
        this.logFileName = builder.logFileName;
        this.logLevel = builder.logLevel;
        this.steps = builder.steps;

        saxon = new Saxon();
        err = new LoggingErrorListener();
        saxon.setErrorListener(err);
    }

    public static PipelineBuilder builder() {
        return new PipelineBuilder();
    }

    public static SubPipeline subPipeline() {
        return new SubPipeline();
    }

    /**
     * Executes the pipeline
     * @return int value indicating no errors, -1 error occurred during execution
     */
    public int exec() {
        int errCode = 0;
        startLogging();
        try {
            IOUtils.safeCreateDirectory(workPath);
            for (Step step : steps) {
                lastOutput = stepExec(step, true);
            }
            if (lastOutput instanceof List && ((List) lastOutput).size() >0 && ((List) lastOutput).get(0) instanceof String) {
                for (Object outputFile : (List) lastOutput) {
                    String path = (String) outputFile;
                    if (IOUtils.pathExists(path)) {
                        try {
                            Files.copy(Paths.get(path), Paths.get(outputPath + IOUtils.filenameFromPath(path)), REPLACE_EXISTING);
                        } catch (IOException ie) {
                            log("Could not copy output file " + path);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            errCode = -1;
            log(String.format("--- Exception in process step %s: %s \n %s", stepCounter, ex.getMessage(), ex.getStackTrace()));
            if (verbose) {
                ex.printStackTrace();
            }
        }
        finishLogging();
        if (archive) {
            try {
                ZIPUtils.plainZipFiles(IOUtils.pathCombine(outputPath, dateString + "_work.zip"), generatedFiles);
            } catch (IOException ie) {
                if (verbose)
                    System.out.println(String.format("Exception while zipping work directory %s", ie.getMessage()));
            }
        }
        if (cleanup) {
            generatedFiles.forEach(IOUtils::safeDeleteFile);
        }
        return errCode;
    }

    void log(String text) {
        if (verbose) {
            System.out.println(text);
            logFileFinal.add(text);
        }
    }

    void finalLogFileAdd(String text) {
        logFileFinal.add(text);
    }

    void logFileAddArray(FileArray array) {
        logFile.addFileArray(array);
    }

    FileArray getErrFileArray() {
        return err.getFileArray();
    }

    Object getLastOutput() {
        return lastOutput;
    }

    String getInputPath() {
        return inputPath;
    }

    String getWorkPath() {
        return workPath;
    }

    void incErrorCounter(int add) {
        errorCounter += add;
    }

    boolean isVerbose() {
        return verbose;
    }

    int getCounter() {
        return stepCounter;
    }

    Object getStepOutput(int step) throws IllegalArgumentException {
        if (step > steps.size() -1)
            throw new IllegalArgumentException(String.format("Referenced step %s not defined.", step));
        return steps.get(step).getActualOutput();
    }

    void saxonTransform(String sourceFile, String xsltFile, String resultFile, String... parameter) {
        saxon.transform(sourceFile, xsltFile, resultFile, parameter);
    }

    Object saxonXQuery(String query, String contextFile, String... params) throws SaxonApiException {
        return saxon.xquery(query, contextFile, params);
    }

    void addGeneratedFile(String file) {
        generatedFiles.add(file);
    }

    void addGeneratedFiles(List<String> files) {
        generatedFiles.addAll(files);
    }

    private static Step createStep(StepType type, Object input, Object output, Object additional, String... params)
            throws IllegalArgumentException
    {
        Step step;
        switch (type) {
            case XSLT:
                step = new XSLTStep(input, output, additional, params);
                break;
            case ZIP:
                step = new ZIPStep(input, output, additional, params);
                break;
            case UNZIP:
                step = new UnzipStep(input, output, additional, params);
                break;
            case XQUERY:
                step = new XQueryStep(input, output, additional, params);
                break;
            case XML_CSV:
                step = new XMLToCSVStep(input, output, additional, params);
                break;
            case COMBINE:
                step = new CombineStep(input, output, additional, params);
                break;
            case PDF_SPLIT:
                step = new PDFSplitStep(input, output, additional, params);
                break;
            case EXIF:
                step = new EXIFStep(input, output, additional, params);
                break;
            case MD5:
                step = new MD5Step(input, output, additional, params);
                break;
            case MD5_FILTER:
                step = new MD5FilterStep(input, output, additional, params);
                break;
            case FILTER:
                step = new FilterStep(input, output, additional, params);
                break;
            case FTP_UP:
                step = new FTPUpStep(input, output, additional, params);
                break;
            case FTP_DOWN:
                step = new FTPDownStep(input, output, additional, params);
                break;
            default:
                step = new EmptyStep(input, output, additional, params);
        }
        step.assertParameters();
        return step;
    }

    private Object stepExec(Step step, boolean mainPipe) throws Exception {
        stepCounter += 1;
        log("## Process Step Number " + stepCounter + (mainPipe ? "" : "(side pipeline)"));
        log("##   Type               : " + step.getType());
        return step.exec(this);
    }

    void execSubPipe(SubPipeline subPipeline) throws Exception {
        while (subPipeline.hasNext()) {
            subPipeline.setOutput( stepExec(subPipeline.getNext(), false) );
        }
    }

    private void startLogging() {
        log("--- Settings Start -----------------------------------------------------------------------");
        log("Process Work Path      = " + workPath);
        log("Input File             = " + inputFile);
        log("Output Path            = " + outputPath);
        //log("myOutputFile   = " + outputFile + "/" + outputFile);
        log("--- Settings end -------------------------------------------------------------------------");
        log("");
        log("--- Log Summary Start --------------------------------------------------------------------");
    }

    void addLogSectionXsl(String sectionName, FileArray currentErrLog) {
        if (currentErrLog.getSize() > 0) {
            logFile.add("");
            logFile.add("###  " + sectionName + " Start #####");
            logFile.add("###  " + currentErrLog.getSize() + " messages ###");
            logFile.addFileArray(currentErrLog);
            logFile.add("###  " + sectionName + " End #####");
            logFile.add("");
        }
        currentErrLog.clear();
    }

    private void finishLogging() {
        System.out.println("### Finishing Converter #####");

        logFileFinal.add("--- Log Summary End ----------------------------------------------------------------------");
        logFileFinal.add("");
        logFileFinal.add("");
        logFileFinal.addFileArray(logFile);

        if (errorCounter > 0) logFileName = logFileName + "_error";
        // special pipeline log file
        logFileFinal.saveFileArray(workPath + "/" + logFileName);
        // logger framework, could be sent to remote server by SocketAppender
        Logging.log(LOGGER, logLevel, String.join("\n", logFileFinal.getContent()) );
    }



    public static class PipelineBuilder {

        private boolean verbose = false;
        private boolean archive = false;
        private boolean cleanup = false;
        private String dateString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        private String workPath = App.TEMP_FILE_PATH + File.separator + dateString;
        private String inputPath = workPath;
        private String outputPath = workPath;
        private String inputFile = "";
        private String logFileName = dateString + ".log";
        private String logLevel = Logging.NONE;
        private List<Step> steps = new ArrayList<>();

        private PipelineBuilder() {}

        public PipelineBuilder setWorkPath(String workPath) {
            this.workPath = workPath;
            return this;
        }

        public PipelineBuilder setInputPath(String inputPath) {
            this.inputPath = inputPath;
            return this;
        }

        public PipelineBuilder setOutputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public PipelineBuilder setLogFile(String logFileName) {
            this.logFileName = logFileName;
            return this;
        }

        public PipelineBuilder setLogLevel(String logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public PipelineBuilder step(StepType type, Object input, Object output, Object additional, String... params) throws IllegalArgumentException {
            if (steps.size() == 0) {
                if (StringUtils.isEmpty(input)) {
                    throw new IllegalArgumentException("Input of first argument must not be null!");
                } else {
                    if (input instanceof String) {
                        inputFile = (String) input;
                    } else if (input instanceof List && (((List) input).get(0) instanceof String)) {
                        inputFile = (String) ((List) input).get(0);
                    }
                }
            }
            steps.add(Pipeline.createStep(type, input, output, additional, params));
            return this;
        }

        public PipelineBuilder verbose() {
            verbose = true;
            return this;
        }

        public PipelineBuilder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public PipelineBuilder archive() {
            archive = true;
            return this;
        }

        public PipelineBuilder archive(boolean archive) {
            this.archive = archive;
            return this;
        }

        public PipelineBuilder cleanup() {
            cleanup = true;
            return this;
        }

        public PipelineBuilder cleanup(boolean cleanup) {
            this.cleanup = cleanup;
            return this;
        }

        public Pipeline build() {
            return new Pipeline(this);
        }
    }


    public static class SubPipeline {
        private List<Step> steps = new ArrayList<>();
        private Object output = null;
        private int pointer = 0;

        private SubPipeline() {}

        public SubPipeline step(StepType type, Object input, Object output, Object additional, String... params) {
            if (steps.size() == 0 && StringUtils.isEmpty(input))
                throw new IllegalArgumentException("Input of first argument must not be null!");
            steps.add(Pipeline.createStep(type, input, output, additional, params));
            return this;
        }

        boolean hasNext() {
            return steps.size() > pointer;
        }

        Step getNext() {
            pointer += 1;
            return steps.size() >= pointer ? steps.get(pointer - 1) :
                    Pipeline.createStep(StepType.NONE, null, null, null);
        }

        Object getOutput() {
            return output;
        }

        private void setOutput(Object output) {
            this.output = output;
            if (pointer < steps.size() && StringUtils.isEmpty( steps.get(pointer).getInput() )) {
                steps.get(pointer).setInput(output);
            }
        }
    }


    public enum StepType {
        XSLT, XSL_FO, XQUERY, XML_CSV, ZIP, UNZIP, EXIF, PDF_SPLIT, PDF_MERGE, THUMB, MD5, MD5_FILTER, COMBINE, CMD,
        FILTER, HTTP_POST, HTTP_GET, FTP_UP, FTP_DOWN, FTP_GRAB, NONE
    }

}
