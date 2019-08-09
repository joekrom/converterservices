package de.axxepta.converterservices.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Monitor {

    private static final long MEGABYTE = 1024L * 1024L;

    public static void appendMemoryUsageLog(String fileName) {
        Runtime runtime = Runtime.getRuntime();
        long memoryUsage = (runtime.totalMemory() - runtime.freeMemory()) / MEGABYTE;
        String line = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()) + ";" + memoryUsage;
        File file = new File(fileName);
        try {
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(file, file.exists() && file.isFile()))) {
                writer.println(line);
            }
        } catch (IOException ex) {
            //
        }
    }
}
