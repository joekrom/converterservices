package de.axxepta.converterservices.tools;

import de.axxepta.converterservices.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.axxepta.converterservices.App.TEMP_FILE_PATH;

public class CmdUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmdUtils.class);

    private CmdUtils() {}

    public static List<String> exec(String cmdLine) throws IOException, InterruptedException {
        List<String> lines = new ArrayList<>();
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec((IOUtils.isWin() ? "cmd /c " : "") +
                cmdLine);
        try (BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
            String line;
            while ((line = input.readLine()) != null) {
                lines.add(line);
            }
            int exitVal = pr.waitFor();
            lines.add(0, Integer.toString(exitVal));
        }
        return lines;
    }

    public static List<String> runProcess(List<String> cmd, Map<String, String> env, String path) throws IOException, InterruptedException {
        if (IOUtils.isWin()) {
            cmd.add(0, "/c");
            cmd.add(0, "cmd");

        }
        ProcessBuilder pb = new ProcessBuilder(cmd).
                redirectErrorStream(true);
        if (env.size() > 0) {
            Map<String, String> envSettings = pb.environment();
            envSettings.putAll(env);
        }
        if (!path.equals("")) {
            pb.directory(new File(path));
        }
        Process pr = pb.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
             BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream())))
        {
            String line;
            while ((line = input.readLine()) != null) {
                lines.add(line);
            }
            while ((line = error.readLine()) != null) {
                lines.add(line);
            }
            int exitVal = pr.waitFor();
            lines.add(0, Integer.toString(exitVal));
        }
        return lines;
    }

    public static ByteArrayOutputStream exif(boolean compact, String option, String file) throws IOException, InterruptedException {
        // ToDo: check output for file names with umlauts (FileName element)
        return compact ?
                (IOUtils.isWin() ? runExternal("exiftool", "-charset filename=cp1252", "-e", option, TEMP_FILE_PATH + "/" + file) :
                    runExternal("exiftool", "-e", option, TEMP_FILE_PATH + "/" + file)) :
                (IOUtils.isWin() ? runExternal("exiftool", "-charset filename=cp1252", option, TEMP_FILE_PATH + "/" + file) :
                        runExternal("exiftool", option, TEMP_FILE_PATH + "/" + file));
    }

    public static List<String> exifPipe(boolean compact, String option, String file) throws IOException, InterruptedException {
        return compact ?
                exec("exiftool " + (IOUtils.isWin() ? "-charset filename=cp1252 " : "") + "-e " + option + " \"" + file + "\"") :
                //exec("@chcp 1252 & exiftool " + (IOUtils.isWin() ? "-charset filename=cp1252 " : "") + option + " \"" + file + "\"");
                exec("exiftool " + (IOUtils.isWin() ? "-charset filename=cp1252 " : "") + option + " \"" + file + "\"");
    }

    public static ByteArrayOutputStream runExternal(String... command) throws IOException, InterruptedException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            Process process = Runtime.getRuntime().
                    exec((IOUtils.isWin() ? "cmd /c " : "") + command);
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
