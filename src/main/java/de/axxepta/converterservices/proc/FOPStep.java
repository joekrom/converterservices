package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.utils.StringUtils;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

class FOPStep extends Step {

    FOPStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.XSL_FO;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters) throws Exception {
        String inputFile = inputFiles.get(0);

        String outputFile = pipe.getWorkPath() +
                (StringUtils.isNoStringOrEmpty(output) ?
                "step_" + pipe.getCounter() + ".pdf" : (String) output);

        String configFile = pipedPath(additional, pipe);

        String outputType = MimeConstants.MIME_PDF;
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1 && (parts[0].toLowerCase().contains("mime") || parts[0].toLowerCase().contains("out"))) {
                String val = parts[1].toLowerCase();
                if (val.contains("jpg"))
                    outputType = MimeConstants.MIME_JPEG;
                if (val.contains("png"))
                    outputType = MimeConstants.MIME_PNG;
                if (val.contains("gif"))
                    outputType = MimeConstants.MIME_GIF;
                if (val.contains("postscript") || val.equals("ps"))
                    outputType = MimeConstants.MIME_POSTSCRIPT;
            }
        }

        FopFactory fopFactory = FopFactory.newInstance(new File(configFile));
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outputFile)))) {
            Fop fop = fopFactory.newFop(outputType, out);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            Source src = new StreamSource(new File(inputFile));
            Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, res);
        }

        actualOutput = outputFile;
        return singleFileList(outputFile);
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return true;
    }

}
