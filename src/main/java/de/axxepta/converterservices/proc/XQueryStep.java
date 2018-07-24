package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;
import org.w3c.dom.Document;

import java.util.List;

class XQueryStep extends Step {

    XQueryStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.XQUERY;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        String queryFile = pipedPath(additional, pipe);

        String query = IOUtils.loadStringFromFile(queryFile);

        Object queryOutput = pipe.saxonXQuery(query,
                input.equals(Saxon.XQUERY_NO_CONTEXT) ? (String) input : inputFiles.get(0),
                parameters);
        if (queryOutput instanceof Document) {
            String outputFile = StringUtils.isNoStringOrEmpty(output) ?
                    IOUtils.pathCombine(pipe.getWorkPath(),"output_step" + pipe.getCounter() + ".xml") :
                    IOUtils.pathCombine(pipe.getWorkPath(), (String) output);
            Saxon.saveDOM((Document) queryOutput, outputFile, "ISO-8859-1");
            pipe.addGeneratedFile(outputFile);
            return singleFileList(outputFile);
        } else {
            // ToDo: check cases, assure correct feeding in pipe
            return queryOutput;
        }
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        switch (paramType) {
            case INPUT:
                break;
            case OUTPUT:
                break;
            case ADDITIONAL:
                return (param instanceof String && !param.equals("")) ||
                        param instanceof Integer ||
                        (param instanceof List && ((List) param).size() > 0 && ((List) param).get(0) instanceof String );
            case PARAMS:
                break;
            default: return true;
        }
        return true;
        //if (StringUtils.isNoStringOrEmpty(param) && paramType.equals(Parameter.ADDITIONAL))
        //    return false;
        //return (param instanceof String) || ((param instanceof List) && ((List) param).get(0) instanceof String);
    }
}
