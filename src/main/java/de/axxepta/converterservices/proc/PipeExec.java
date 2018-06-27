package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.utils.IOUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Can be called as command line pipeline processing tool.
 * Provide an XML pipeline description file as first parameter (see in the Wiki for more information).
 */
public class PipeExec {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipeExec.class);

    private final static String PIPE_ELEMENT = "pipeline";
    private final static String STEP_ELEMENT = "step";
    private final static String NAME_ELEMENT = "name";
    private final static String INPUT_ELEMENT = "input";
    private final static String OUTPUT_ELEMENT = "output";
    private final static String ADD_ELEMENT = "add";
    private final static String PARAM_ELEMENT = "param";

    private final static String VERBOSE = "verbose";
    private final static String ARCHIVE = "archive";
    private final static String CLEANUP = "cleanup";
    private final static String WORK_PATH = "workPath";
    private final static String INPUT_PATH = "inputPath";
    private final static String OUTPUT_PATH = "outputPath";
    private final static String LOG_FILE = "logFile";
    private final static String LOG_LEVEL = "logLevel";

    /**
     * Attribute name for step type (can have the values of Pipeline.StepType) and for step parameter types.
     * Step parameter types can have the values bool/boolean, int/integer, long, float, double, or string
     * in upper or lower cases
     */
    private final static String TYPE = "type";


    public static void main(String[] args) {
        if (args.length != 0) {
            try {
                int r = execProcessFile(args[0]);
                System.out.println(String.format("Pipeline execution finished with return code %s", r));
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
     * @return Pipeline process execution code as returned by PipelineBuilder.exec()
     * @throws IllegalArgumentException if the pipeline description contains invalid components
     * @throws NullPointerException if the step type attribute is missing
     * @throws ParserConfigurationException if the XML description structure is not valid XML
     * @throws IOException if no XML DOM can be build from the description
     * @throws SAXException if a XSLT step fails
     * @throws XPathExpressionException if the XPath processing for pipeline parsing fails
     */
    public static int execProcessFile(String file)
            throws IllegalArgumentException, NullPointerException, ParserConfigurationException, IOException,
            SAXException, XPathExpressionException
    {
        if (IOUtils.pathExists(file) && !IOUtils.isDirectory(file)) {
            Document dom = Saxon.loadDOM(file);
            return execProcess(dom);
        } else {
            throw new IllegalArgumentException("File does not exist");
        }
    }

    /**
     * Executes a pipeline described in a passed String. Look in the wiki for further information.
     * @param xmlString Pipeline description input XML String
     * @return Pipeline process execution code as returned by PipelineBuilder.exec()
     * @throws IllegalArgumentException if the pipeline description contains invalid components
     * @throws NullPointerException if the step type attribute is missing
     * @throws ParserConfigurationException if the XML description structure is not valid XML
     * @throws IOException if no XML DOM can be build from the description
     * @throws SAXException if a XSLT step fails
     * @throws XPathExpressionException if the XPath processing for pipeline parsing fails
     */
    public static int execProcessString(String xmlString)
            throws IllegalArgumentException, NullPointerException, ParserConfigurationException, IOException,
            SAXException, XPathExpressionException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xmlString));
        Document dom = builder.parse(is);
        return execProcess(dom);
    }

    /**
     * Executes a pipeline described in a passed XML DOM. Look in the wiki for further information.
     * @param dom Pipeline description as XML DOM
     * @return Pipeline process execution code as returned by PipelineBuilder.exec()
     * @throws XPathExpressionException - if the XPath processing for pipeline parsing fails
     * @throws NullPointerException - if the step type attribute is missing
     * @throws IllegalArgumentException - if the pipeline description contains invalid components
     */
    public static int execProcess(Document dom) throws XPathExpressionException, NullPointerException, IllegalArgumentException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        Node pipeNode = (Node) xPath.compile("//" + PIPE_ELEMENT).evaluate(dom, XPathConstants.NODE);
        if (pipeNode != null) {
            Pipeline.PipelineBuilder pipelineBuilder = Pipeline.builder();
            NamedNodeMap pipeAttributes = pipeNode.getAttributes();
            pipelineBuilder = evalSettings(pipelineBuilder, pipeAttributes);
            NodeList steps = (NodeList) xPath.compile("./" + STEP_ELEMENT).evaluate(pipeNode, XPathConstants.NODESET);
            for (int s = 0; s < steps.getLength(); s++) {
                pipelineBuilder = evalSteps(pipelineBuilder, steps.item(s), xPath);
            }
            return pipelineBuilder.exec();
        } else {
            throw new XPathExpressionException("No pipeline defined in input.");
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
                    builder = builder.setLogLevel(att.getNodeValue());
                    break;
            }
        }
        return builder;
    }

    private static Pipeline.PipelineBuilder evalSteps(Pipeline.PipelineBuilder builder, Node step, XPath xPath)
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
        String[] param = (String[]) assignParameter(step, xPath, PARAM_ELEMENT);

        return builder.step(type, stepName, input, output, additional, param);
    }

    private static Object assignParameter(Node step, XPath xPath, String path)
            throws XPathExpressionException, NullPointerException, IllegalArgumentException
    {
        Object param = null;
        NodeList nodes = (NodeList) xPath.compile("./" + path).evaluate(step, XPathConstants.NODESET);
        if (nodes.getLength() > 0) {
            Node typeAtt = nodes.item(0).getAttributes().getNamedItem(TYPE);
            String type = (typeAtt == null) ? "String" : typeAtt.getNodeValue();
            if (nodes.getLength() == 1) {
                String nodeContent = nodes.item(0).getTextContent();
                switch (type.toLowerCase()) {
                    case "bool":
                    case "boolean":
                        param = Boolean.valueOf(nodeContent);
                        break;
                    case "int":
                    case "integer":
                        param = Integer.valueOf(nodeContent);
                        break;
                    case "long":
                        param = Long.valueOf(nodeContent);
                        break;
                    case "byte":
                        param = Byte.valueOf(nodeContent);
                        break;
                    case "float":
                        param = Float.valueOf(nodeContent);
                        break;
                    case "double":
                        param = Double.valueOf(nodeContent);
                        break;
                    default: param = nodeContent;
                }
                if (path.equals(PARAM_ELEMENT)) {
                    param = new String[] { (String) param };
                }
            } else {
                List<String> nodeContents = new ArrayList<>();
                for (int i = 0; i < nodes.getLength(); i++) {
                    nodeContents.add(nodes.item(i).getTextContent());
                }
                List list = new ArrayList<>();
                switch (type.toLowerCase()) {
                    case "bool":
                    case "boolean":
                        list.addAll(nodeContents.stream().map(Boolean::valueOf).collect(Collectors.toList()));
                        break;
                    case "int":
                    case "integer":
                        list.addAll(nodeContents.stream().map(Integer::valueOf).collect(Collectors.toList()));
                        break;
                    case "long":
                        list.addAll(nodeContents.stream().map(Long::valueOf).collect(Collectors.toList()));
                        break;
                    case "byte":
                        list.addAll(nodeContents.stream().map(Byte::valueOf).collect(Collectors.toList()));
                        break;
                    case "float":
                        list.addAll(nodeContents.stream().map(Float::valueOf).collect(Collectors.toList()));
                        break;
                    case "double":
                        list.addAll(nodeContents.stream().map(Double::valueOf).collect(Collectors.toList()));
                        break;
                    default:
                        list.addAll(nodeContents);
                }
                if (path.equals(PARAM_ELEMENT)) {
                    param = list.toArray(new String[list.size()]);
                } else {
                    param = list;
                }
            }
        } else if (path.equals(PARAM_ELEMENT)) {
            return new String[0];
        }
        return param;
    }

    private static Pipeline.StepType evalStepType(Node typeAtt) {
        if (typeAtt == null) {
            LOGGER.warn("Step lacks type definition, step will be ignored.");
            return Pipeline.StepType.NONE;
        } else {
            try {
                return Pipeline.StepType.valueOf(typeAtt.getNodeValue());
            } catch (NullPointerException | IllegalArgumentException ex) {
                LOGGER.warn(String.format("Type %s does not correspond to a defined step type, step will be ignored.",
                        typeAtt.getNodeValue()));
                return Pipeline.StepType.NONE;
            }
        }
    }
}
