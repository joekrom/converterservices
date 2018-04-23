package de.axxepta.converterservices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZIPUtils {

    public static byte[] getZippedRenamedFiles(String tempFile, List<String> inputFiles, List<String> names)
            throws IOException {
        Path tempPath = Paths.get(tempFile);
        File zipFileName = tempPath.toFile();
        try(ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFileName))) {
            for (int i = 0; i < inputFiles.size(); i++) {
                addToZipFile(zipStream, inputFiles.get(i), names.get(i));
            }
        }
        return Files.readAllBytes(tempPath);
    }

    public static byte[] getRenamedZippedFiles(String tempFile, String inputFiles, String names) throws IOException {
        String[] inputs = nlListToArray(inputFiles);
        String[] outputs = nlListToArray(names);
        return getZippedRenamedFiles(tempFile, Arrays.asList(inputs), Arrays.asList(outputs));
    }

    private static void addToZipFile(ZipOutputStream zipStream, String input, String name) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(input)) {
            ZipEntry entry = new ZipEntry(name);
            zipStream.putNextEntry(entry);
            byte[] readBuffer = new byte[1024];
            int amountRead;
            while ((amountRead = inputStream.read(readBuffer)) > 0) {
                zipStream.write(readBuffer, 0, amountRead);
            }
        }
    }

    private static String[] nlListToArray(String nlList) {
        return nlList.split("\\r?\\n");
    }
}
