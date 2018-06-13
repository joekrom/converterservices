package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.IOUtils;
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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PipeExec {

    private final static String PIPE_ELEMENT = "pipeline";
    private final static String STEP_ELEMENT = "step";
    private final static String INPUT_ELEMENT = "input";
    private final static String OUTPUT_ELEMENT = "output";
    private final static String ADD_ELEMENT = "add";
    private final static String PARAM_ELEMENT = "param";

    private final static String VERBOSE = "verbose";
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

    public static int execProcessFile(String file)
            throws IllegalArgumentException, NullPointerException, ParserConfigurationException, IOException,
            SAXException, XPathExpressionException
    {
        if (IOUtils.fileExists(file) && !IOUtils.isDirectory(file)) {
            File xmlFile = new File(file);
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document dom = builder.parse(xmlFile);
            return execProcess(dom);
        } else {
            throw new IllegalArgumentException("File does not exist");
        }
    }

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
            return pipelineBuilder.build().exec();
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

        Object input = assignParameter(step, xPath, INPUT_ELEMENT);
        Object output = assignParameter(step, xPath, OUTPUT_ELEMENT);
        Object additional = assignParameter(step, xPath, ADD_ELEMENT);
        Object param = assignParameter(step, xPath, PARAM_ELEMENT);

        return builder.step(type, input, output, additional, param);
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
                param = list;
            }
        }
        return param;
    }

    private static Pipeline.StepType evalStepType(Node typeAtt) {
        try {
            return Pipeline.StepType.valueOf(typeAtt.getNodeValue());
        } catch (NullPointerException|IllegalArgumentException ex) {
            return Pipeline.StepType.NONE;
        }
    }
}
