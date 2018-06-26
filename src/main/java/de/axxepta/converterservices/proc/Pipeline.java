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

    private final boolean verbose;
    private final boolean archive;
    private final boolean cleanup;
    private final String dateString;
    private final String workPath;
    private final String inputPath;
    private final String outputPath;
    private final String inputFile;
    private final String logFileName;
    private final String logLevel;
    private final List<Step> steps;

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

    /**
     * Executes the pipeline
     * @return int value indicating no errors, -1 error occurred during execution
     */
    private int exec() {
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
                            Files.copy(Paths.get(path),
                                    Paths.get(IOUtils.pathCombine(outputPath, IOUtils.filenameFromPath(path))),
                                    REPLACE_EXISTING);
                        } catch (IOException ie) {
                            log("Could not copy output file " + path);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            errCode = -1;
            log(String.format("--- Exception in process step %s: %s", stepCounter, ex.getMessage()));
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

    Object getStepOutput(String stepName) throws IllegalArgumentException {
        for (Step step : steps) {
            if (step.getName().equals(stepName)) {
                return step.getActualOutput();
            }
        }
        throw new IllegalArgumentException(String.format("Referenced step %s not defined.", stepName));
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

    private static Step createStep(final StepType type, String name, int no, final Object input, final Object output,
                                   final Object additional, final String... params) throws IllegalArgumentException
    {
        if (StringUtils.isEmpty(name)) {
            name = Integer.toString(no);
        }
        Step step;
        switch (type) {
            case XSLT:
                step = new XSLTStep(name, input, output, additional, params);
                break;
            case ZIP:
                step = new ZIPStep(name, input, output, additional, params);
                break;
            case UNZIP:
                step = new UnzipStep(name, input, output, additional, params);
                break;
            case XQUERY:
                step = new XQueryStep(name, input, output, additional, params);
                break;
            case XML_CSV:
                step = new XMLToCSVStep(name, input, output, additional, params);
                break;
            case COMBINE:
                step = new CombineStep(name, input, output, additional, params);
                break;
            case PDF_SPLIT:
                step = new PDFSplitStep(name, input, output, additional, params);
                break;
            case EXIF:
                step = new EXIFStep(name, input, output, additional, params);
                break;
            case MD5:
                step = new MD5Step(name, input, output, additional, params);
                break;
            case MD5_FILTER:
                step = new MD5FilterStep(name, input, output, additional, params);
                break;
            case FILTER:
                step = new FilterStep(name, input, output, additional, params);
                break;
            case FTP_UP:
                step = new FTPUpStep(name, input, output, additional, params);
                break;
            case FTP_DOWN:
                step = new FTPDownStep(name, input, output, additional, params);
                break;
            case HTTP_POST:
                step = new HTTPPostStep(name, input, output, additional, params);
                break;
            case CMD:
                step = new CmdStep(name, input, output, additional, params);
                break;
            case LIST:
                step = new ListStep(name, input, output, additional, params);
                break;
            default:
                step = new EmptyStep(name, input, output, additional, params);
        }
        step.assertParameters(no);
        return step;
    }

    private Object stepExec(Step step, boolean mainPipe) throws Exception {
        stepCounter += 1;
        log("## Process Step Number " + stepCounter + (mainPipe ? "" : "(side pipeline)"));
        log("##   Type               : " + step.getType());
        return step.exec(this);
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

        String finalLogFileName = errorCounter > 0 ? logFileName + "_error" : logFileName;
        // special pipeline log file
        logFileFinal.saveFileArray(workPath + "/" + finalLogFileName);
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

        public PipelineBuilder step(StepType type, String name, Object input, Object output, Object additional, String... params)
                throws IllegalArgumentException
        {
            if (steps.size() == 0) {
                if ((input instanceof String && !input.equals("")) ||
                        (input instanceof List && ((List) input).get(0) instanceof String) && !((List) input).get(0).equals("") )
                {
                    if (input instanceof String) {
                        inputFile = (String) input;
                    } else {
                        inputFile = (String) ((List) input).get(0);
                    }
                } else {
                    throw new IllegalArgumentException("Input of first argument must not be null!");
                }
            }
            steps.add(Pipeline.createStep(type, name, steps.size(), input, output, additional, params));
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

        public int exec() {
            return new Pipeline(this).exec();
        }
    }


    public enum StepType {
        XSLT, XSL_FO, XQUERY, XML_CSV, ZIP, UNZIP, EXIF, PDF_SPLIT, PDF_MERGE, THUMB, MD5, MD5_FILTER, COMBINE, CMD,
        FILTER, HTTP_POST, HTTP_GET, FTP_UP, FTP_DOWN, FTP_GRAB, LIST, NONE
    }

}
