package de.axxepta.converterservices;

import de.axxepta.converterservices.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Core {


    private static final Logger LOGGER = LoggerFactory.getLogger(Core.class);

    private static final List<String> activeDirectories = new ArrayList<>();

    private Core() {}

    static void cleanTemp(List<String> files) {
        for (String file : files) {
            try {
                Files.delete(Paths.get(Const.TEMP_FILE_PATH + "/" + file));
            } catch (IOException ex) {
                if (LOGGER != null) LOGGER.error(ex.getMessage());
            }
        }
    }

    public static String setTempPath(String ... basePath) {
        String dateString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        synchronized (activeDirectories) {
            if (activeDirectories.contains(dateString)) {
                dateString += "__" + activeDirectories.size();
            }
            activeDirectories.add(dateString);
        }
        try {
            IOUtils.safeCreateDirectory(
                    IOUtils.pathCombine(basePath.length > 0 ? basePath[0] : Const.TEMP_FILE_PATH, dateString)
            );
        } catch (IOException ex) {
            return "";
        }
        return dateString;
    }

    public static void cleanup(String dateString, String ... basePath) {
        try {
            FileUtils.deleteDirectory(
                    new File(
                            IOUtils.pathCombine(basePath.length > 0 ? basePath[0] : Const.TEMP_FILE_PATH, dateString)
                    )
            );
        } catch (IOException ex) {
            LOGGER.warn("Error while deleting directory: ", ex);
        }
        releaseTemporaryDir(dateString);
    }

    public static void releaseTemporaryDir(String dateString) {
        synchronized (activeDirectories) {
            activeDirectories.remove(dateString);
        }
    }
}
