package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.CmdUtils;
import de.axxepta.converterservices.utils.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class CmdStep extends Step {

    CmdStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.CMD;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        Map<String, String> env = new HashMap<>();
        String cmdLine = parameters[0];
        String path = "";
        List<String> win = new ArrayList<>();
        List<String> lin = new ArrayList<>();
        List<String> all = new ArrayList<>();
        for (String parameter : parameters) {
            String[] parts = parameter.split(" *= *");
            if (parts.length > 1) {
                String paramVal = parameter.substring(parameter.indexOf(parts[1], parameter.indexOf("=")));
                switch (parts[0].toLowerCase()) {
                    case "env": case "environment": case "set":
                        int eqPos = paramVal.indexOf("=");
                        if (eqPos != -1) {
                            env.put(parts[1], paramVal.substring(eqPos + 1));
                        }
                        break;
                    case "path": case "dir":
                        path = parts[1];
                        break;
                    case "win":
                        win.add(parts[1]);
                        break;
                    case "lin":
                        lin.add(parts[1]);
                        break;
                    case "all":
                        all.add(parts[1]);
                        break;
                }
            }
        }

        List<String> outputFiles = new ArrayList<>();
        int i = 0;
        int outSize = 0;
        List<String> outputs = new ArrayList<>();
        try {
            outputs = (List) output;
            outSize = outputs.size();
        } catch (Exception cc) {}

        for (String inFile : inputFiles) {
            String outputFile = (inputFiles.size() == outSize) ?
                    IOUtils.pathCombine(pipe.getWorkPath(), outputs.get(i)) :
                    IOUtils.pathCombine(pipe.getWorkPath(), IOUtils.filenameFromPath(inFile) + ".step");

            try {
                List<String> lines;

                if (win.size() == 0 && lin.size() == 0 && all.size() == 0) {
                    // single command line parameter, without OS specific components, use %s as placeholder for input file
                    lines = CmdUtils.exec(String.format(cmdLine, "\"" + inFile + "\""));

                } else {
                    // complex command line parameters, use %1 as placeholder for input file, as in batch files, more options see below
                    List<String> cmds = IOUtils.isWin() ? win : lin;
                    cmds.addAll(all);
                    // replace some batch command line arguments extensions
                    cmds = cmds.stream().map(c -> c.replace("%1", inFile)).collect(Collectors.toList());
                    cmds = cmds.stream().map(c -> c.replace("%~n1", IOUtils.strippedFilename(inFile))).collect(Collectors.toList());
                    cmds = cmds.stream().map(c -> c.replace("%~x1", "." + IOUtils.getFileExtension(inFile))).collect(Collectors.toList());
                    cmds = cmds.stream().map(c -> c.replace("%~nx1", IOUtils.filenameFromPath(inFile))).collect(Collectors.toList());
                    cmds = cmds.stream().map(c -> c.replace("%~dp1", IOUtils.dirFromPath(inFile))).collect(Collectors.toList());

                    cmdLine = cmds.stream().collect(Collectors.joining(" "));
                    if (!IOUtils.isWin()) {
                        cmds.clear();
                        cmds.add(cmdLine);
                    }
                    lines = CmdUtils.runProcess(cmds, env, path);

                }
                if (lines.size() > 0 && !lines.get(0).equals("0")) {
                    pipe.log("External process exited with error code " + lines.get(0));
                }
                IOUtils.saveStringArrayToFile(lines.size() > 1 ? lines.subList(1, lines.size() - 1) : lines,
                        outputFile, false);

            } catch (IOException | InterruptedException ex) {
                pipe.log(String.format("Error executing external command %s:\n %s", cmdLine, ex.getMessage()));
            }

            pipe.addGeneratedFile(outputFile);
            outputFiles.add(outputFile);
            i++;
        }
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        switch (paramType) {
            case INPUT:
                return assertStandardInput(param);
            case OUTPUT:
                return assertStandardOutput(param);
            case PARAMS:
                return ((String[]) param).length > 0;
        }
        return true;
    }
}
