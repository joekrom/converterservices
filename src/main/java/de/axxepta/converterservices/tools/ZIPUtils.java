package de.axxepta.converterservices.tools;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZIPUtils {

    public static byte[] getZippedRenamedFiles(String tempFile, List<String> inputFiles, List<String> names)
            throws IOException {
        Path tempPath = Paths.get(tempFile);
        zipRenamedFiles(tempFile, inputFiles, names);
        return Files.readAllBytes(tempPath);
    }

    public static void zipRenamedFiles(String tempFile, List<String> inputFiles, List<String> names)
            throws IOException {
        Path tempPath = Paths.get(tempFile);
        File zipFileName = tempPath.toFile();
        try(ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFileName))) {
            for (int i = 0; i < inputFiles.size(); i++) {
                addToZipFile(zipStream, inputFiles.get(i), names.get(i));
            }
        }
    }

    public static void plainZipFiles(String zipFile, List<String> inputFiles) throws IOException {
        List<String> names = inputFiles.stream().map(IOUtils::filenameFromPath).collect(Collectors.toList());
        zipRenamedFiles(zipFile, inputFiles, names);
    }

    public static byte[] getRenamedZippedFiles(String tempFile, String inputFiles, String names) throws IOException {
        String[] inputs = StringUtils.nlListToArray(inputFiles);
        String[] outputs = StringUtils.nlListToArray(names);
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

}
