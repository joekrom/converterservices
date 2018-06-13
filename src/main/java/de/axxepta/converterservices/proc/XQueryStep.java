package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.Saxon;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;
import org.w3c.dom.Document;

import java.util.List;

class XQueryStep extends Step {

    XQueryStep(Object input, Object output, Object additional, Object params) {
        super(input, output, additional, params);
    }

    Pipeline.StepType getType() {
        return Pipeline.StepType.XQUERY;
    }

    @Override
    Object execAction(List<String> inputFiles, Object additionalInput, Object parameters, Pipeline pipe) throws Exception {
        String queryFile = pipedPath(additionalInput, pipe);
        String query = IOUtils.readTextFile(queryFile);
        Object queryOutput = pipe.saxonXQuery(query,
                input.equals(Saxon.XQUERY_NO_CONTEXT) ? (String) input : inputFiles.get(0),
                (String) params);
        if (queryOutput instanceof Document) {
            String outputFile = StringUtils.isEmpty(output) ? "output_step" + pipe.getCounter() + ".xml" : (String) output;
            Saxon.saveDOM((Document) queryOutput, outputFile);
            pipe.addGeneratedFile(outputFile);
            return singleFileList(outputFile);
        } else {
            // ToDo: check cases, assure correct feeding in pipe
            return queryOutput;
        }
    }

    @Override
    boolean assertParameter(Parameter paramType, Object param) {
        if (StringUtils.isEmpty(param) && paramType.equals(Parameter.ADDITIONAL))
            return false;
        return (param instanceof String) || ((param instanceof List) && ((List) param).get(0) instanceof String);
    }
}
