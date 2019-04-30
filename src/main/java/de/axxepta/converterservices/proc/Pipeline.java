package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.App;
import de.axxepta.converterservices.Core;
import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.tools.ZIPUtils;
import de.axxepta.converterservices.utils.*;
import net.sf.saxon.s9api.SaxonApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Pipeline {

    /**
     * "Protocol" identifier prefix for inputs to be taken from working directory
     */
    public static final String WORK_DIR = "pipe://";

    /**
     * "Protocol" identifier prefix for inputs to be taken from previously executed named step
     */
    public static final String NAMED_STEP = "step://";

    /**
     * "Protocol" identifier prefix for inputs to be interpreted as absolute paths
     */
    public static final String FILE_INPUT = "file://";

    /**
     * "Protocol" identifier prefix for inputs in the input path to be expanded as regular expression
     */
    public static final String REGEXP_INPUT = "regexp://";

    private static final Logger LOGGER = LoggerFactory.getLogger(Pipeline.class);

    private final boolean verbose;
    private final boolean archive;
    private final boolean cleanup;
    private final String dateString;
    private String workPath;
    private final String inputPath;
    private final String outputPath;
    private final String inputFile;
    private String logFileName;
    private final String logLevel;

    private final String ftpHost;
    private final String ftpUser;
    private final String ftpPwd;
    private final int ftpPort;
    private final boolean ftpSecure;

    private final String httpHost;
    private final String httpUser;
    private final String httpPwd;
    private final int httpPort;
    private final boolean httpSecure;

    private final String mailHost;
    private final String mailUser;
    private final String mailPwd;
    private final int mailPort;
    private final boolean mailSecure;
    private final String mailSender;

    private final List<Step> steps;
    private final List<Step> errorSteps;
    private boolean errorPipeRunning;

    private String temporaryWorkPath = "";

    private int stepCounter = 0;
    private int errorCounter = 0;

    private Saxon saxon;
    private LoggingErrorListener err;
    private FileArray logFileFinal = new FileArray();
    private FileArray logFile = new FileArray();

    private List<String> generatedFiles = new ArrayList<>();
    private Object lastOutput = new ArrayList<>();

    private Pipeline(PipelineBuilder builder, String extWorkPath) {
        this.verbose = builder.verbose;
        this.archive = builder.archive;
        this.cleanup = builder.cleanup;
        this.workPath = builder.workPath;
        this.inputPath = builder.inputPath;
        this.outputPath = builder.outputPath;
        this.inputFile = builder.inputFile;
        this.logFileName = builder.logFileName;
        this.logLevel = builder.logLevel;

        this.ftpHost = builder.ftpHost;
        this.ftpUser = builder.ftpUser;
        this.ftpPwd = builder.ftpPwd;
        this.ftpPort = builder.ftpPort;
        this.ftpSecure = builder.ftpSecure;

        this.httpHost = builder.httpHost;
        this.httpUser = builder.httpUser;
        this.httpPwd = builder.httpPwd;
        this.httpPort = builder.httpPort;
        this.httpSecure = builder.httpSecure;

        this.mailHost = builder.mailHost;
        this.mailUser = builder.mailUser;
        this.mailPwd = builder.mailPwd;
        this.mailPort = builder.mailPort;
        this.mailSecure = builder.mailSecure;
        this.mailSender = builder.mailSender;

        this.steps = builder.steps;
        this.errorSteps = builder.errorSteps;
        errorPipeRunning = false;

        saxon = new Saxon();
        err = new LoggingErrorListener();
        saxon.setErrorListener(err);

        if (workPath.equals("")) {
            if (extWorkPath.equals("")) {
                temporaryWorkPath = Core.setTempPath();
                dateString = temporaryWorkPath;
                workPath = IOUtils.pathCombine(App.TEMP_FILE_PATH, temporaryWorkPath);
            } else {
                workPath = extWorkPath;
                dateString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            }
        } else {
            dateString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        }

        if (logFileName.equals("")) {
            logFileName = dateString + ".log";
        }

    }

    /**
     * Creates a PipelineBuilder instance which can be used to define an XML processing pipeline.
     * @return Empty PipelineBuilder
     */
    public static PipelineBuilder builder() {
        return new PipelineBuilder();
    }

    /*
     * @return Last step's output or Integer with value -1 if an error occurred during Pipeline execution code
     */
    private Object exec() {
        Integer errCode = 0;
        startLogging();
        try {
            IOUtils.safeCreateDirectory(workPath);
            IOUtils.safeCreateDirectory(outputPath);
            for (Step step : steps) {
                lastOutput = stepExec(step, false);
            }
            if (!outputPath.equals("") && lastOutput instanceof List && ((List) lastOutput).size() > 0
                    && ((List) lastOutput).get(0) instanceof String) {
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
            log("#--# Step " + stepCounter + " # Exception: " + ex.getMessage());
            if (verbose) {
                ex.printStackTrace();
            }
        } finally {
            if (!temporaryWorkPath.equals("")) {
                Core.releaseTemporaryDir(temporaryWorkPath);
            }
        }
        String finalLogFile = finishLogging();
        if (archive) {
            try {
                ZIPUtils.plainZipFiles(IOUtils.pathCombine(outputPath, dateString + "_work.zip"), generatedFiles);
            } catch (IOException ie) {
                if (verbose)
                    System.out.println(String.format("Exception while zipping work directory %s", ie.getMessage()));
            }
        }
        if (errCode.equals(-1)) {
            stepCounter = 0;
            errorPipeRunning = true;
            if (errorSteps.size() > 0 && errorSteps.get(0).getInput().equals("")) {
                errorSteps.get(0).setInput(finalLogFile);
            }
            try {
                for (Step step : errorSteps) {
                    lastOutput = stepExec(step, true);
                }
            } catch (Exception ex) {
                if (verbose) {
                    ex.printStackTrace();
                }
            }
        }
        if (cleanup) {
            generatedFiles.forEach(IOUtils::safeDeleteFile);
        }
        return errCode.equals(0) ? lastOutput : errCode;
    }

    public void log(String text) {
        if (verbose) {
            System.out.println(text);
            logFileFinal.add(text);
        }
    }

    void finalLogFileAdd(String text) {
        logFileFinal.add("#--# Step " + stepCounter + " # " + text);
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

    String getFtpHost() {
        return ftpHost;
    }

    String getFtpUser() {
        return ftpUser;
    }

    String getFtpPwd() {
        return ftpPwd;
    }

    int getFtpPort() {
        return ftpPort;
    }

    boolean isFtpSecure() {
        return ftpSecure;
    }

    String getHttpHost() {
        return httpHost;
    }

    String getHttpUser() {
        return httpUser;
    }

    String getHttpPwd() {
        return httpPwd;
    }

    int getHttpPort() {
        return httpPort;
    }

    boolean isHttpSecure() {
        return httpSecure;
    }

    String getMailHost() {
        return mailHost;
    }

    String getMailUser() {
        return mailUser;
    }

    String getMailPwd() {
        return mailPwd;
    }

    int getMailPort() {
        return mailPort;
    }

    boolean isMailSecure() {
        return mailSecure;
    }

    String getMailSender() {
        return mailSender;
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
        if (step >= stepCounter)
            throw new IllegalArgumentException(String.format("Referenced step %s not defined or not yet executed.", step));
        return errorPipeRunning ?
                errorSteps.get(step).getActualOutput() :
                steps.get(step).getActualOutput();
    }

    Object getStepOutput(String stepName) throws IllegalArgumentException {
        for (int i = 0; i < stepCounter; i++) {
            Step step = errorPipeRunning ? errorSteps.get(i) : steps.get(i);
            if (step.getName().equals(stepName)) {
                return step.getActualOutput();
            }
        }
        throw new IllegalArgumentException(String.format("Referenced step %s not defined or not yet executed.", stepName));
    }

    void saxonTransform(String sourceFile, String xsltFile, String resultFile, String... parameter) throws TransformerException {
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
                                   final Object additional, final boolean stopOnError, final String... params) throws IllegalArgumentException
    {
        if (StringUtils.isEmpty(name)) {
            name = Integer.toString(no);
        }
        Step step;
        switch (type) {
            case XSLT:
                step = new XSLTStep(name, input, output, additional, stopOnError, params);
                break;
            case XSL_FO:
                step = new FOPStep(name, input, output, additional, stopOnError, params);
                break;
            case JSON_XML:
                step = new JSONtoXMLStep(name, input, output, additional, stopOnError, params);
                break;
            case ZIP:
                step = new ZIPStep(name, input, output, additional, stopOnError, params);
                break;
            case UNZIP:
                step = new UnzipStep(name, input, output, additional, stopOnError, params);
                break;
            case GUNZIP:
                step = new GUnzipStep(name, input, output, additional, stopOnError, params);
                break;
            case XQUERY:
                step = new XQueryStep(name, input, output, additional, stopOnError, params);
                break;
            case XML_CSV:
                step = new XMLToCSVStep(name, input, output, additional, stopOnError, params);
                break;
            case XLSX_XML:
                step = new XLSXToXMLStep(name, input, output, additional, stopOnError, params);
                break;
            case XML_XLSX:
                step = new XMLToXLSXStep(name, input, output, additional, stopOnError, params);
                break;
            case COMBINE:
                step = new CombineStep(name, input, output, additional, stopOnError, params);
                break;
            case FT:
                step = new FilenameTransformStep(name, input, output, additional, stopOnError, params);
                break;
            case PDF_SPLIT:
                step = new PDFSplitStep(name, input, output, additional, stopOnError, params);
                break;
            case EXIF:
                step = new EXIFStep(name, input, output, additional, stopOnError, params);
                break;
            case MD5:
                step = new MD5Step(name, input, output, additional, stopOnError, params);
                break;
            case MD5_FILTER:
                step = new MD5FilterStep(name, input, output, additional, stopOnError, params);
                break;
            case FILTER:
                step = new FilterStep(name, input, output, additional, stopOnError, params);
                break;
            case FTP_UP:
                step = new FTPUpStep(name, input, output, additional, stopOnError, params);
                break;
            case FTP_DOWN:
                step = new FTPDownStep(name, input, output, additional, stopOnError, params);
                break;
            case HTTP_POST:
                step = new HTTPPostStep(name, input, output, additional, stopOnError, params);
                break;
            case HTTP_GET:
                step = new HTTPGetStep(name, input, output, additional, stopOnError, params);
                break;
            case CMD:
                step = new CmdStep(name, input, output, additional, stopOnError, params);
                break;
            case LIST:
                step = new ListStep(name, input, output, additional, stopOnError, params);
                break;
            case REPLACE:
                step = new ReplaceStep(name, input, output, additional, stopOnError, params);
                break;
            case BASE64_DEC:
                step = new Base64DecodeStep(name, input, output, additional, stopOnError, params);
                break;
            case MAIL:
                step = new MailStep(name, input, output, additional, stopOnError, params);
                break;
            default:
                step = new EmptyStep(name, input, output, additional, stopOnError, params);
        }
        step.assertParameters(no);
        return step;
    }

    private Object stepExec(Step step, boolean error) throws Exception {
        stepCounter += 1;
        log("## " + (error ? "Error" : "Process") + " Step Number " + stepCounter);
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

    private String finishLogging() {
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
        return finalLogFileName;
    }


    public static class PipelineBuilder {

        private boolean verbose = false;
        private boolean archive = false;
        private boolean cleanup = false;
        private String workPath = "";
        private String inputPath = workPath;
        private String outputPath = "";
        private String inputFile = "";
        private String logFileName = "";
        private String logLevel = Logging.NONE;

        private String ftpHost = "";
        private String ftpUser = "";
        private String ftpPwd = "";
        private int ftpPort = 0;
        private boolean ftpSecure = true;

        private String httpHost = "";
        private String httpUser = "";
        private String httpPwd = "";
        private int httpPort = 0;
        private boolean httpSecure = true;

        private String mailHost = "";
        private String mailUser = "";
        private String mailPwd = "";
        private int mailPort = 0;
        private boolean mailSecure = true;
        private String mailSender = "";

        private List<Step> steps = new ArrayList<>();
        private List<Step> errorSteps = new ArrayList<>();

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

        public PipelineBuilder setFtpHost(String ftpHost) {
            this.ftpHost = ftpHost;
            return this;
        }

        public PipelineBuilder setFtpUser(String ftpUser) {
            this.ftpUser = ftpUser;
            return this;
        }

        public PipelineBuilder setFtpPwd(String ftpPwd) {
            this.ftpPwd = ftpPwd;
            return this;
        }

        public PipelineBuilder setFtpPort(int ftpPort) {
            this.ftpPort = ftpPort;
            return this;
        }

        public PipelineBuilder setFtpSecure(boolean ftpSecure) {
            this.ftpSecure = ftpSecure;
            return this;
        }

        public PipelineBuilder setHttpHost(String httpHost) {
            this.httpHost = httpHost;
            return this;
        }

        public PipelineBuilder setHttpUser(String httpUser) {
            this.httpUser = httpUser;
            return this;
        }

        public PipelineBuilder setHttpPwd(String httpPwd) {
            this.httpPwd = httpPwd;
            return this;
        }

        public PipelineBuilder setHttpPort(int httpPort) {
            this.httpPort = httpPort;
            return this;
        }

        public PipelineBuilder setHttpSecure(boolean httpSecure) {
            this.httpSecure = httpSecure;
            return this;
        }

        public PipelineBuilder setMailHost(String mailHost) {
            this.mailHost = mailHost;
            return this;
        }

        public PipelineBuilder setMailUser(String mailUser) {
            this.mailUser = mailUser;
            return this;
        }

        public PipelineBuilder setMailPwd(String mailPwd) {
            this.mailPwd = mailPwd;
            return this;
        }

        public PipelineBuilder setMailPort(int mailPort) {
            this.mailPort = mailPort;
            return this;
        }

        public PipelineBuilder setMailSecure(boolean mailSecure) {
            this.mailSecure = mailSecure;
            return this;
        }

        public PipelineBuilder setMailSender(String mailSender) {
            this.mailSender = mailSender;
            return this;
        }

        /**
         * Defines steps of an XML processing pipeline, will add the step to a list to be executed, but does not actually
         * perform the processing.
         * @param type Type of transformation or processing
         * @param name Optional name, can be null or empty String. Can be referenced from later steps in the definition
         *             of inputs or additional inputs with a prepended <i>step://</i> to the parameters to feed the output
         *             of this step at the corresponding later point.
         * @param input Input file(s) as String or List of Strings, relative to the input path of the pipe, can be null
         *             or empty String if the output of the previous step shall be used. Using a prefix <i>regexp://</i>
         *             will filter the input path for a regular expression provided as rest of the input.
         *             An Integer with value <i>n</i> will reference the output files of the <i>n</i>th step in the pipe.
         *             A prepended <i>pipe://</i> will reference file names relative to the work path of the pipe, a
         *             prepended <i>step://</i> will reference all output files of a previous named step. A prepended
         *             <i>file://</i> will result in interpretation of the name as absolute. Glob syntax can be used.
         * @param output String or List of String, optional output file name(s), can be null or empty String
         *              (standard values will be generated).
         *              Provided file names have to be relative to the working path.
         * @param additional Possible additional file names which can be relative to the input path, work path (if
         *                   prepended with <i>step://</i>, <i>pipe://</i> or provided as Integer, see input) or relative
         *                   to other references (e.g. path on FTP server or in ZIP file). See in the Wiki for more details.
         * @param stopOnError Defines whether pipeline execution should be stopped after an error in this step
         * @param params (Optional) parameters, see in the Wiki for possible values for different StepTypes
         * @return Pipeline builder with current step appended to step list
         * @throws IllegalArgumentException If one of the provided arguments does not fit the expected type. This prevents
         *          the pipe from being executed with false parameters.
         */
        public PipelineBuilder step(StepType type, String name, Object input, Object output, Object additional,
                                    boolean stopOnError, String... params)
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
            steps.add(Pipeline.createStep(type, name, steps.size(), input, output, additional, stopOnError, params));
            return this;
        }

        /**
         * Defines error steps to be executed if the processing pipeline failed to succeed, will add the step to a list
         * to be executed, but does not actually perform the processing.
         * @param type Type of transformation or processing
         * @param name Optional name, can be null or empty String. Can be referenced from later steps in the definition
         *             of inputs or additional inputs with a prepended <i>step://</i> to the parameters to feed the output
         *             of this step at the corresponding later point.
         * @param input Input file(s) as String or List of Strings, relative to the input path of the pipe, can be null
         *             or empty String if the output of the previous step shall be used. Using a prefix <i>regexp://</i>
         *             will filter the input path for a regular expression provided as rest of the input.
         *             An Integer with value <i>n</i> will reference the output files of the <i>n</i>th step in the error pipe.
         *             A prepended <i>pipe://</i> will reference file names relative to the work path of the pipe, a
         *             prepended <i>step://</i> will reference all output files of a previous named error step. A prepended
         *             <i>file://</i> will result in interpretation of the name as absolute. Glob syntax can be used.
         * @param output String or List of String, optional output file name(s), can be null or empty String
         *              (standard values will be generated).
         *              Provided file names have to be relative to the working path.
         * @param additional Possible additional file names which can be relative to the input path, work path (if
         *                   prepended with <i>step://</i>, <i>pipe://</i> or provided as Integer, see input) or relative
         *                   to other references (e.g. path on FTP server or in ZIP file). See in the Wiki for more details.
         * @param stopOnError Defines whether error pipeline execution should be stopped after an error in this step
         * @param params (Optional) parameters, see in the Wiki for possible values for different StepTypes
         * @return Pipeline builder with current step appended to error step list
         * @throws IllegalArgumentException If one of the provided arguments does not fit the expected type. This prevents
         *          the pipe from being executed with false parameters.
         */
        public PipelineBuilder errorStep(StepType type, String name, Object input, Object output, Object additional,
                                    boolean stopOnError, String... params)
                throws IllegalArgumentException
        {
            errorSteps.add(Pipeline.createStep(type, name, errorSteps.size(), input, output, additional, stopOnError, params));
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

        //ToDo: build?

        /**
         * Executes the pipeline defined by the builder. This "consumes" the pipeline, no actual instance of it is returned.
         * You can execute a new one from the last returned builder.
         * @param externalWorkPath Injected work path. Is only used if none is defined in the pipe itself. If neither implicit
         *                         nor external is provided, a temporary time stamped directory will be created in the work path
         *                         of the application.
         * @return Last step's output or Integer with value -1 if an error occurred during Pipeline execution code
         */
        public Object exec(String... externalWorkPath) {
            return new Pipeline(this, externalWorkPath.length > 0 ? externalWorkPath[0] : "").exec();
        }
    }


    public enum StepType {
        XSLT, XSL_FO, XQUERY, XML_CSV, XML_XLSX, ZIP, UNZIP, FT, GZIP, GUNZIP, EXIF, PDF_SPLIT, PDF_MERGE, THUMB, MD5, MD5_FILTER,
        COMBINE, CMD, JSON_XML, FILTER, HTTP_POST, HTTP_GET, FTP_UP, FTP_DOWN, FTP_GRAB, LIST, REPLACE, MAIL,
        BASE64_ENC, BASE64_DEC, XLSX_XML, NONE
    }

}
