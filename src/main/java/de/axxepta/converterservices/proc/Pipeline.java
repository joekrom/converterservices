package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.App;
import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.tools.ZIPUtils;
import de.axxepta.converterservices.utils.FileArray;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.LoggingErrorListener;
import de.axxepta.converterservices.utils.StringUtils;
import net.sf.saxon.s9api.SaxonApiException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Pipeline {

    private boolean verbose;
    private String dateString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    private String workPath = App.TEMP_FILE_PATH + File.separator + dateString;
    private String inputPath = workPath;
    private String outputPath = workPath;
    private String logFileName = workPath + File.separator + dateString + ".log";
    private List<Step> steps = new ArrayList<>();

    private Pipeline() {}

    public static Pipeline builder() {
        return new Pipeline();
    }

    public static SubPipeline subPipeline() {
        return new SubPipeline();
    }

    public Pipeline setWorkPath(String workPath) {
        this.workPath = workPath;
        return this;
    }

    public Pipeline setInputPath(String inputPath) {
        this.inputPath = inputPath;
        return this;
    }

    public Pipeline setOutputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public Pipeline setLogFile(String logFileName) {
        this.logFileName = logFileName;
        return this;
    }

    public Pipeline step(Step.Type type, Object input, Object output, Object additional, Object params) throws IllegalArgumentException {
        if (steps.size() == 0 && StringUtils.isEmpty(input))
            throw new IllegalArgumentException("Input of first argument must not be null!");
        if (!Step.assertParameter(type, Step.Parameter.ADDITIONAL, additional))
            throw new IllegalArgumentException("Wrong additional input type!");
        if (!Step.assertParameter(type, Step.Parameter.PARAMS, params))
            throw new IllegalArgumentException("Wrong process parameter type!");
        if (!Step.assertParameter(type, Step.Parameter.INPUT, input))
            throw new IllegalArgumentException("Wrong input type!");
        if (!Step.assertParameter(type, Step.Parameter.OUTPUT, input))
            throw new IllegalArgumentException("Wrong output type!");
        Step step = new Step(type, input, output, additional, params);
        steps.add(step);
        return this;
    }

    public Pipeline verbose() {
        verbose = true;
        return this;
    }

    public int exec() {
        return new PipeRun().exec();
    }


    class PipeRun {

        private int stepCounter = 0;
        private int errorCounter = 0;

        private Saxon saxon;
        LoggingErrorListener err;
        private FileArray logFileFinal = new FileArray();
        private FileArray logFile = new FileArray();

        private List<String> generatedFiles = new ArrayList<>();
        private Object lastOutput = new ArrayList<>();

        PipeRun() {
            saxon = new Saxon();
            err = new LoggingErrorListener();
            saxon.setErrorListener(err);
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

        int getCounter() {
            return stepCounter;
        }

        void saxonTransform(String sourceFile, String xsltFile, String resultFile, String parameter) {
            saxon.transform(sourceFile, xsltFile, resultFile, parameter);
        }

        Object saxonXQuery(String query, String contextFile, String params) throws SaxonApiException {
            return saxon.xquery(query, contextFile, params);
        }

        void addGeneratedFile(String file) {
            generatedFiles.add(file);
        }

        private int exec() {
            int errCode = 0;
            startLogging();
            try {
                IOUtils.safeCreateDirectory(workPath);
                for (Step step : steps) {
                    lastOutput = stepExec(step, true);
                }
            } catch (Exception ex) {
                errCode = -1;
                logFileFinal.add(String.format("--- Exception in process step %s: %s", stepCounter, ex.getMessage()));
            }
            finishLogging();
            try {
                ZIPUtils.plainZipFiles(outputPath + dateString + "_work.zip", generatedFiles);
            } catch (IOException ie) {
                if (verbose)
                    System.out.println(String.format("Exception while zipping work directory %s", ie.getMessage()));
            }
            generatedFiles.forEach(IOUtils::safeDeleteFile);
            return errCode;
        }

        private Object stepExec(Step step, boolean mainPipe) throws Exception {
            stepCounter += 1;
            if (verbose) {
                System.out.println("## Process step number " + stepCounter + (mainPipe ? "" : "(side pipeline)"));
                System.out.println("## Process type:       " + step.getType());
            }
            return step.exec(this);
        }

        void execSubPipe(SubPipeline subPipeline) throws Exception {
            while (subPipeline.hasNext()) {
                subPipeline.setOutput( stepExec(subPipeline.getNext(), false) );
            }
        }

        private void startLogging() {
            if (verbose) {
                System.out.println("### Settings Start ###");
                //    System.out.println("Process workPath    = " + processPath);
                //    System.out.println("Input File      = " + inputFile);
                //    System.out.println("XSL folder      = " + xslFolder);
                System.out.println("Output folder   = " + workPath);
                //    System.out.println("Output file     = " + outputFolder + "/" + outputFile);
                System.out.println("### Settings end ###");
            }

            logFileFinal.add("--- Settings Start -----------------------------------------------------------------------");
            //logFileFinal.add("Process workPath      = " + processPath);
            //logFileFinal.add("inputFile             = " + inputFile);
            //logFileFinal.add("myXslFolder           = " + xslFolder);
            logFileFinal.add("Output folder  = " + workPath);
            //logFileFinal.add("myOutputFile   = " + outputFile + "/" + outputFile);
            logFileFinal.add("--- Settings end -------------------------------------------------------------------------");
            logFileFinal.add("");
            logFileFinal.add("--- Log Summary Start --------------------------------------------------------------------");
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
            logFileFinal.saveFileArray(workPath + "/" + logFileName);
        }

    }


    public static class PipelineBuilder {
        private PipelineBuilder() {}
    }

    public static class SubPipeline {
        private List<Step> steps = new ArrayList<>();
        private Object output = null;
        private int pointer = 0;

        private SubPipeline() {}

        public SubPipeline step(Step.Type type, Object input, Object output, Object additional, Object params) {
            Step step = new Step(type, input, output, additional, params);
            steps.add(step);
            return this;
        }

        boolean hasNext() {
            return steps.size() > pointer;
        }

        Step getNext() {
            pointer += 1;
            return steps.size() >= pointer ? steps.get(pointer - 1) :
                    new Step(Step.Type.NONE, null, null, null, null);
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

}
