package de.axxepta.converterservices.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CmdUtils {

    private CmdUtils() {}
    public static List<String> exec(String cmdLine) throws IOException, InterruptedException {
        List<String> lines = new ArrayList<>();
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec((System.getProperty("os.name").startsWith("Windows") ? "cmd /c " : "") +
                cmdLine);
        try (BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {

            String line = null;

            while ((line = input.readLine()) != null) {
                lines.add(line);
            }
            int exitVal = pr.waitFor();
            lines.add(0, Integer.toString(exitVal));
        }
        return lines;
    }
}
