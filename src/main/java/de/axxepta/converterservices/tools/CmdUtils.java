package de.axxepta.converterservices.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static de.axxepta.converterservices.App.TEMP_FILE_PATH;

public class CmdUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmdUtils.class);

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

    public static ByteArrayOutputStream exif(boolean compact, String option, String file) throws IOException, InterruptedException {
        return compact ?
                runExternal("exiftool", "-e", option, TEMP_FILE_PATH + "/" + file) :
                runExternal("exiftool", option, TEMP_FILE_PATH + "/" + file);
    }

    public static ByteArrayOutputStream exifPipe(boolean compact, String option, String file) throws IOException, InterruptedException {
        return compact ?
                runExternal("exiftool", "-e", option, file) :
                runExternal("exiftool", option, file);
    }

    public static ByteArrayOutputStream runExternal(String... command) throws IOException, InterruptedException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            Process process = Runtime.getRuntime().exec(command);
            try (InputStream is = process.getInputStream()) {
                int n;
                while ((n = is.read(buffer)) > -1) {
                    stream.write(buffer, 0, n);
                }
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    LOGGER.error(line);
                }
            }
            int code = process.waitFor();
            LOGGER.debug("External program returned with code " + code);
        } catch (IOException | InterruptedException err) {
            LOGGER.error(err.getMessage());
            throw err;
        }
        return stream;
    }
}
