package de.axxepta.converterservices.tools;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.s9api.*;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Saxon {

    private ErrorListener _err;
    private Processor processor = new Processor(false);

    public static final String XQUERY_NO_CONTEXT = "NO:CONTEXT";
    public static final String XQUERY_OUTPUT = ":OUTPUT:";
    public static final String XS_BOOLEAN = "xs:boolean";
    public static final String XS_INT = "xs:int";
    public static final String XS_FLOAT = "xs:float";
    public static final String XS_STRING = "xs:string";
    public static final String NODE = "node()";

    public void setErrorListener(ErrorListener err){
        _err = err;
    }

    public void transform(String sourceFile, String xsltFile, String resultFile, String... parameters) {
        transform(sourceFile, xsltFile, resultFile, "false", parameters);
    }

    private void transform(String sourceFile, String xsltFile, String resultFile, String validateDTD, String... parameters) {

        TransformerFactory tFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        try {
            tFactory.setAttribute(FeatureKeys.MESSAGE_EMITTER_CLASS, "net.sf.saxon.serialize.MessageWarner");
            tFactory.setAttribute(FeatureKeys.DTD_VALIDATION, validateDTD);
            tFactory.setAttribute(FeatureKeys.EXPAND_ATTRIBUTE_DEFAULTS, "false");

            if(_err != null) tFactory.setErrorListener(_err);
        } catch(Exception e) {
            System.out.println("Error setting transformer factory attributes " + e.getMessage());
        }
        try {
            Transformer transformer =  tFactory.newTransformer(new StreamSource(new File(xsltFile)));

            if (parameters.length > 0) {
                for (String singleParam : parameters) {
                    String param[] = singleParam.split("=");
                    transformer.setParameter(param[0], param[1]);
                }
            }
            transformer.transform(new StreamSource(new File(sourceFile)), new StreamResult(new File(resultFile)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean validateXML(String xmlFile, String ngFile){
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = parser.parse(new File(xmlFile));
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.RELAXNG_NS_URI);
            Schema schema = factory.newSchema(new File(ngFile));
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(document));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    public static String standardOutputFilename(String xsltPath) {
        String file = IOUtils.filenameFromPath(xsltPath);
        int pos = file.contains(".") ? file.indexOf(".") : file.length();
        return file.substring(0, pos) + ".xml";
    }

    private static Document getDOM() throws SaxonApiException {
        Document dom;
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        try {
            return dFactory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new SaxonApiException(e);
        }
    }

    private static List xqueryListOutput(XdmValue result, String type) throws SaxonApiException {
        List output = new ArrayList();
        for (XdmItem item : result) {
            switch (type) {
                case XS_BOOLEAN: output.add(new Boolean(((XdmAtomicValue) item).getBooleanValue()));
                    break;
                case XS_FLOAT: output.add(new Double(((XdmAtomicValue) item).getDoubleValue()));
                    break;
                case XS_INT: output.add(new Long(((XdmAtomicValue) item).getLongValue()));
                    break;
                default: output.add(item.getStringValue());
            }
        }
        return output;
    }

    public Object execXQuery(String query, XdmItem context, Map<QName, XdmItem> bindings, String outputType) throws SaxonApiException {
        Object output;
        XQueryCompiler comp = processor.newXQueryCompiler();
        XQueryExecutable exp = comp.compile(query);
        XQueryEvaluator qe = exp.load();
        for (Map.Entry<QName, XdmItem> binding : bindings.entrySet()) {
            qe.setExternalVariable(binding.getKey(), binding.getValue());
        }
        if (context != null) {
            qe.setContextItem(context);
        }
        if (outputType.equals(NODE)) {
            Document dom = getDOM();
            qe.run(new DOMDestination(dom));
            output = dom;
        } else {
            XdmValue result = qe.evaluate();
            output = xqueryListOutput(result, outputType);
        }
        return output;
    }

    /**
     *
     * @param query The query, possibly containing binding definitions
     * @param contextFile An XML file's path which will be loaded as context of the query or, if none provided, the
     *                   constant <code>Saxon.XQUERY_NO_CONTEXT</code>.
     * @param params Variable bindings and output type in a new-line separated String. Each line is expected to contain
     *               a binding definition in the form <code>varName = value as type</code>,
     *               the optional output definition in the form <code>Saxon.XQUERY_OUTPUT as type</code>.
     *               The standard output type is DOM, denoted by Saxon.NODE.
     * @throws SaxonApiException
     */
    public Object xquery(String query, String contextFile, String... params) throws SaxonApiException{
        List<String> bindings = new ArrayList<>(Arrays.asList(params));
        String outputType = NODE;
        for (int i = bindings.size() - 1; i >= 0; i--) {
            String line = bindings.get(i);
            if (line.startsWith(XQUERY_OUTPUT)) {
                outputType = line.substring(line.lastIndexOf(" as ") + 4);
                bindings.remove(i);
            }
        }
        return xquery(query, contextFile, bindings, outputType);
    }

    public Object xquery(String query, String contextFile, List<String> bindingStrings, String outputType) throws SaxonApiException{
        if (StringUtils.isEmpty(outputType)) {
            outputType = NODE;
        }
        Map<QName, XdmItem> bindings = new HashMap<>();
        for (String line : bindingStrings) {
            String name = line.split(" *= *")[0];
            int endPos = line.lastIndexOf(" as ");
            String type = endPos == -1 ? XS_STRING : line.substring(endPos + 4);
            String val = line.substring(0, endPos).split(" *= *")[1];
            bindings.put(new QName(name), bindingVal(val, type));
        }
        XdmItem context = null;
        if (!contextFile.equals(XQUERY_NO_CONTEXT)) {
            context = processor.newDocumentBuilder().build(new StreamSource(new File(contextFile)));
        }
        return execXQuery(query, context, bindings, outputType);
    }

    private XdmItem bindingVal(String val, String type) throws SaxonApiException {
        switch (type) {
            case XS_BOOLEAN: return val.toLowerCase().contains("true") ? new XdmAtomicValue(true) : new XdmAtomicValue(false);
            case XS_FLOAT: return new XdmAtomicValue(Float.parseFloat(val));
            case XS_INT: return new XdmAtomicValue(Integer.parseInt(val));
            case NODE: return processor.newDocumentBuilder().build(new StreamSource(new File(val)));
            default: return new XdmAtomicValue(val);
        }
    }

    public static void saveDOM(Document dom, String fileName) throws TransformerException, IOException {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult fileResult = new StreamResult(new FileWriter(fileName));
            transformer.transform(new DOMSource(dom), fileResult);
    }
}
