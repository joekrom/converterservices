package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Can be called as command line pipeline processing tool.
 * Provide an XML pipeline description file as first parameter (see in the Wiki for more information).
 */
public class PipeExec {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipeExec.class);

    private final static String PIPE_ELEMENT = "pipeline";
    private final static String STEP_ELEMENT = "step";
    private final static String ERROR_STEP_ELEMENT = "error";
    private final static String NAME_ELEMENT = "name";
    private final static String INPUT_ELEMENT = "input";
    private final static String OUTPUT_ELEMENT = "output";
    private final static String ADD_ELEMENT = "add";
    private final static String STOP_ELEMENT = "stop";
    private final static String PARAM_ELEMENT = "param";

    private final static String VERBOSE = "verbose";
    private final static String ARCHIVE = "archive";
    private final static String CLEANUP = "cleanup";
    private final static String COPY_LOG = "copyLog";
    private final static String EXCEPTION_HANDLER = "useExceptionHandler";
    private final static String WORK_PATH = "workPath";
    private final static String INPUT_PATH = "inputPath";
    private final static String OUTPUT_PATH = "outputPath";
    private final static String LOG_FILE = "logFile";
    private final static String LOG_LEVEL = "logLevel";


    private final static String FTP_HOST = "ftpHost";
    private final static String FTP_USER = "ftpUser";
    private final static String FTP_PWD = "ftpPwd";
    private final static String FTP_PORT = "ftpPort";
    private final static String FTP_SECURE = "ftpSecure";

    private final static String HTTP_HOST = "httpHost";
    private final static String HTTP_USER = "httpUser";
    private final static String HTTP_PWD = "httpPwd";
    private final static String HTTP_PORT = "httpPort";
    private final static String HTTP_SECURE = "httpSecure";

    private final static String MAIL_HOST = "mailHost";
    private final static String MAIL_USER = "mailUser";
    private final static String MAIL_PWD = "mailPwd";
    private final static String MAIL_PORT = "mailPort";
    private final static String MAIL_SECURE = "mailSecure";
    private final static String MAIL_SENDER = "mailSender";

    private final static String CLASS_ATT = "class";

    /**
     * Attribute name for step type (can have the values of Pipeline.StepType) and for step parameter types.
     * Step parameter types can have the values bool/boolean, int/integer, long, float, double, or string
     * in upper or lower cases
     */
    private final static String TYPE = "type";


    public static void main(String[] args) {
        System.out.println("JAR PATH: " + IOUtils.jarPath());
        System.out.println("EXECUTION CONTEXT PATH: " + IOUtils.executionContextPath());
        if (args.length != 0) {
            try {
                Object result = execProcessFile(args[0]);
                if (result instanceof Integer && result.equals(-1)) {
                    System.out.println("Pipeline execution was not successful. See in the log file for details.");
                } else if (result instanceof String || (result instanceof List && ((List)result).get(0) instanceof String)) {
                    System.out.println("Pipeline output: " + result);
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        } else {
            System.out.println("Provide process description XML file as parameter.");
        }
    }

    /**
     * Executes a pipeline described in a passed file. Look in the wiki for further information.
     * @param file XML pipeline description input file name
     * @param externalWorkPath External work path, will only be used of none is defined in the pipe itself.
     * @return Last step's output or Integer with value -1 if an error occurred during Pipeline execution code
     * @throws IllegalArgumentException if the pipeline description contains invalid components
     * @throws NullPointerException if the step type attribute is missing
     * @throws ParserConfigurationException if the XML description structure is not valid XML
     * @throws IOException if no XML DOM can be build from the description
     * @throws SAXException if a XSLT step fails
     * @throws XPathExpressionException if the XPath processing for pipeline parsing fails
     */
    public static Object execProcessFile(String file, String... externalWorkPath)
            throws IllegalArgumentException, NullPointerException, ParserConfigurationException, IOException,
            SAXException, XPathExpressionException
    {
        if (IOUtils.pathExists(file) && !IOUtils.isDirectory(file)) {
            Document dom = Saxon.loadDOM(file);
            return execProcess(dom, externalWorkPath);
        } else {
            throw new IllegalArgumentException("File does not exist");
        }
    }

    /**
     * Executes a pipeline described in a passed String. Look in the wiki for further information.
     * @param xmlString Pipeline description input XML String
     * @param externalWorkPath External work path, will only be used of none is defined in the pipe itself.
     * @return Last step's output or Integer with value -1 if an error occurred during Pipeline execution code
     * @throws IllegalArgumentException if the pipeline description contains invalid components
     * @throws NullPointerException if the step type attribute is missing
     * @throws ParserConfigurationException if the XML description structure is not valid XML
     * @throws IOException if no XML DOM can be build from the description
     * @throws SAXException if an XSLT step fails
     * @throws XPathExpressionException if the XPath processing for pipeline parsing fails
     */
    public static Object execProcessString(String xmlString, String... externalWorkPath)
            throws IllegalArgumentException, NullPointerException, ParserConfigurationException, IOException,
            SAXException, XPathExpressionException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xmlString));
        Document dom = builder.parse(is);
        return execProcess(dom, externalWorkPath);
    }

    /**
     * Executes a pipeline described in a passed XML DOM. Look in the wiki for further information.
     * @param dom Pipeline description as XML DOM
     * @param externalWorkPath External work path, will only be used of none is defined in the pipe itself.
     * @return Last step's output or Integer with value -1 if an error occurred during Pipeline execution code
     * @throws XPathExpressionException - if the XPath processing for pipeline parsing fails
     * @throws NullPointerException if the step type attribute is missing
     * @throws IllegalArgumentException if the pipeline description contains invalid components
     */
    public static Object execProcess(Document dom, String... externalWorkPath) throws XPathExpressionException, NullPointerException, IllegalArgumentException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        Node pipeNode = (Node) xPath.compile("//" + PIPE_ELEMENT).evaluate(dom, XPathConstants.NODE);
        if (pipeNode != null) {
            NamedNodeMap pipeAttributes = pipeNode.getAttributes();
            Pipeline.PipelineBuilder pipelineBuilder = pipeHasClassDef(pipeAttributes) ?
                    classedPipeBuilder(pipeAttributes.getNamedItem(CLASS_ATT).getNodeValue()) :
                    Pipeline.builder();
            pipelineBuilder = evalSettings(pipelineBuilder, pipeAttributes);
            NodeList steps = (NodeList) xPath.compile("./" + STEP_ELEMENT).evaluate(pipeNode, XPathConstants.NODESET);
            for (int s = 0; s < steps.getLength(); s++) {
                pipelineBuilder = evalSteps(pipelineBuilder, steps.item(s), xPath, false);
            }
            NodeList errorSteps = (NodeList) xPath.compile("./" + ERROR_STEP_ELEMENT).evaluate(pipeNode, XPathConstants.NODESET);
            for (int s = 0; s < errorSteps.getLength(); s++) {
                pipelineBuilder = evalSteps(pipelineBuilder, errorSteps.item(s), xPath, true);
            }
            return pipelineBuilder.exec(externalWorkPath);
        } else {
            throw new XPathExpressionException("No pipeline defined in input.");
        }
    }

    private static boolean pipeHasClassDef(NamedNodeMap settings) {
        return settings.getNamedItem(CLASS_ATT) != null;
    }

    private static Pipeline.PipelineBuilder classedPipeBuilder(String className) {
        try {
            Class<?> builderClass = Class.forName(className);
            if (IPipelineBuilderProvider.class.isAssignableFrom(builderClass)) {
                Constructor<?> constructor = builderClass.getConstructor();
                IPipelineBuilderProvider provider = (IPipelineBuilderProvider) constructor.newInstance();
                return provider.builder();
            } else {
                LOGGER.warn("Referenced class does not implement IPipelineBuilderProvider");
                return Pipeline.builder();
            }
        } catch (Exception ce) {
            LOGGER.warn("Could not create instance of referenced class", ce);
            return Pipeline.builder();
        }
    }

    private static Pipeline.PipelineBuilder evalSettings(Pipeline.PipelineBuilder builder, NamedNodeMap settings) {
        for (int a = 0; a < settings.getLength(); a++) {
            Node att = settings.item(a);
            switch (att.getNodeName()) {
                case VERBOSE:
                    if (att.getNodeValue().toLowerCase().equals("true"))
                        builder = builder.verbose();
                    break;
                case ARCHIVE:
                    if (att.getNodeValue().toLowerCase().equals("true"))
                        builder = builder.archive();
                    break;
                case CLEANUP:
                    if (att.getNodeValue().toLowerCase().equals("true"))
                        builder = builder.cleanup();
                    break;
                case COPY_LOG:
                    if (att.getNodeValue().toLowerCase().equals("true"))
                        builder = builder.copyLog();
                    break;
                case EXCEPTION_HANDLER:
                    if (att.getNodeValue().toLowerCase().equals("true"))
                        builder = builder.useExceptionHandler();
                    break;
                case WORK_PATH:
                    builder = builder.setWorkPath(att.getNodeValue());
                    break;
                case INPUT_PATH:
                    builder = builder.setInputPath(att.getNodeValue());
                    break;
                case OUTPUT_PATH:
                    builder = builder.setOutputPath(att.getNodeValue());
                    break;
                case LOG_FILE:
                    builder = builder.setLogFile(att.getNodeValue());
                    break;
                case LOG_LEVEL:
                    builder = builder.setLogLevel(att.getNodeValue().toUpperCase());
                    break;

                case FTP_HOST:
                    builder = builder.setFtpHost(att.getNodeValue());
                    break;
                case FTP_USER:
                    builder = builder.setFtpUser(att.getNodeValue());
                    break;
                case FTP_PWD:
                    builder = builder.setFtpPwd(att.getNodeValue());
                    break;
                case FTP_PORT:
                    if (StringUtils.isInt(att.getNodeValue()))
                        builder = builder.setFtpPort(Integer.valueOf(att.getNodeValue()));
                    break;
                case FTP_SECURE:
                    if (StringUtils.isBool(att.getNodeValue()))
                        builder = builder.setFtpSecure(Boolean.parseBoolean(att.getNodeValue()));
                    break;
                case HTTP_HOST:
                    builder = builder.setHttpHost(att.getNodeValue());
                    break;
                case HTTP_USER:
                    builder = builder.setHttpUser(att.getNodeValue());
                    break;
                case HTTP_PWD:
                    builder = builder.setHttpPwd(att.getNodeValue());
                    break;
                case HTTP_PORT:
                    if (StringUtils.isInt(att.getNodeValue()))
                        builder = builder.setHttpPort(Integer.valueOf(att.getNodeValue()));
                    break;
                case HTTP_SECURE:
                    if (StringUtils.isBool(att.getNodeValue()))
                        builder = builder.setHttpSecure(Boolean.parseBoolean(att.getNodeValue()));
                    break;
                case MAIL_HOST:
                    builder = builder.setMailHost(att.getNodeValue());
                    break;
                case MAIL_USER:
                    builder = builder.setMailUser(att.getNodeValue());
                    break;
                case MAIL_PWD:
                    builder = builder.setMailPwd(att.getNodeValue());
                    break;
                case MAIL_PORT:
                    if (StringUtils.isInt(att.getNodeValue()))
                        builder = builder.setMailPort(Integer.valueOf(att.getNodeValue()));
                    break;
                case MAIL_SECURE:
                    if (StringUtils.isBool(att.getNodeValue()))
                        builder = builder.setMailSecure(Boolean.parseBoolean(att.getNodeValue()));
                    break;
                case MAIL_SENDER:
                    builder = builder.setMailSender(att.getNodeValue());
                    break;
            }
        }
        return builder;
    }

    private static Pipeline.PipelineBuilder evalSteps(Pipeline.PipelineBuilder builder, Node step, XPath xPath, boolean error)
            throws XPathExpressionException, NullPointerException, IllegalArgumentException
    {
        Pipeline.StepType type = evalStepType(step.getAttributes().getNamedItem(TYPE));

        String stepName;
        Node nameNode = (Node) xPath.compile("./" + NAME_ELEMENT).evaluate(step, XPathConstants.NODE);
        if (nameNode == null) {
            stepName = "";
        } else {
            stepName = nameNode.getTextContent();
        }

        Object input = assignParameter(step, xPath, INPUT_ELEMENT);
        Object output = assignParameter(step, xPath, OUTPUT_ELEMENT);
        Object additional = assignParameter(step, xPath, ADD_ELEMENT);
        boolean stopOnError = (Boolean) assignParameter(step, xPath, STOP_ELEMENT);
        String[] param = (String[]) assignParameter(step, xPath, PARAM_ELEMENT);

        return error ?
                builder.errorStep(type, stepName, input, output, additional, stopOnError, param) :
                builder.step(type, stepName, input, output, additional, stopOnError, param);
    }

    private static Object assignParameter(Node step, XPath xPath, String path)
            throws XPathExpressionException, NullPointerException, IllegalArgumentException
    {
        Object param = null;
        NodeList nodes = (NodeList) xPath.compile("./" + path).evaluate(step, XPathConstants.NODESET);
        if (nodes.getLength() > 0) {
            Node typeAtt = nodes.item(0).getAttributes().getNamedItem(TYPE);
            String type = (typeAtt == null) ? "String" : typeAtt.getNodeValue();
            if (path.equals(STOP_ELEMENT)) {
                type = "boolean";
                param = true;
            }
            if (nodes.getLength() == 1) {
                String nodeContent = nodes.item(0).getTextContent();
                switch (type.toLowerCase()) {
                    case "int":
                    case "integer":
                        param = Integer.valueOf(nodeContent);
                        break;
                    case "bool":
                    case "boolean":
                        param = Boolean.valueOf(nodeContent);
                        break;
                    default: param = nodeContent;
                }
                if (path.equals(PARAM_ELEMENT)) {
                    param = new String[] { (String) param };
                }
            } else {
                if (!(StringUtils.isEmpty(type.toLowerCase()) || type.toLowerCase().equals("string") )) {
                    throw new IllegalArgumentException("Illegal parameter type definition: " + type);
                }
                List<String> list = new ArrayList<>();
                for (int i = 0; i < nodes.getLength(); i++) {
                    list.add(nodes.item(i).getTextContent());
                }
                if (path.equals(PARAM_ELEMENT)) {
                    param = list.toArray(new String[list.size()]);
                } else {
                    param = list;
                }
            }
        } else if (path.equals(PARAM_ELEMENT)) {
            return new String[0];
        } else if (path.equals(STOP_ELEMENT)) {
            return true;
        }
        return param;
    }

    private static Pipeline.StepType evalStepType(Node typeAtt) {
        if (typeAtt == null) {
            LOGGER.warn("Step lacks type definition, step will be ignored.");
            return Pipeline.StepType.NONE;
        } else {
            try {
                return Pipeline.StepType.valueOf(typeAtt.getNodeValue().toUpperCase());
            } catch (NullPointerException | IllegalArgumentException ex) {
                LOGGER.warn(String.format("Type %s does not correspond to a defined step type, step will be ignored.",
                        typeAtt.getNodeValue().toUpperCase()));
                return Pipeline.StepType.NONE;
            }
        }
    }
}
