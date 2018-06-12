package de.axxepta.converterservices.tools;

import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
                if (IOUtils.isDirectory(inputFiles.get(i))) {
                    addDirToZipArchive(zipStream, inputFiles.get(i), IOUtils.dirFromPath(inputFiles.get(i)));
                } else {
                    addToZipFile(zipStream, inputFiles.get(i), names.get(i));
                }
            }
        }
    }

    private static void addDirToZipArchive(ZipOutputStream zipStream, String fileName, String parentDirectoryName) throws IOException {
        File fileToZip = new File(fileName);
        if (!fileToZip.exists()) {
            return;
        }

        String zipEntryName = fileToZip.getName();
        if (parentDirectoryName!=null && !parentDirectoryName.isEmpty()) {
            zipEntryName = parentDirectoryName + "/" + fileToZip.getName();
        }

        if (fileToZip.isDirectory()) {
            for (File file : fileToZip.listFiles()) {
                addDirToZipArchive(zipStream, file.getName(), zipEntryName);
            }
        } else {
            addToZipFile(zipStream, fileName, zipEntryName);
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
            zipStream.closeEntry();
        }
    }

    public static List<String> unzip(String zipFileName, String destinationDir) throws IOException {
        List<String> extractedFiles = new ArrayList<>();
        try ( ZipFile zipFile = new ZipFile(zipFileName) ) {
            for ( ZipEntry entry : Collections.list( zipFile.entries() ) ) {
                extractedFiles.add(entry.getName());
                extractEntry( zipFile, entry, destinationDir );
            }
        }
        return extractedFiles;
    }

    private static void extractEntry(ZipFile zipFile, ZipEntry entry, String destinationDir)
            throws IOException {
        Path destinationPath = Paths.get(destinationDir, entry.getName());
        if (entry.isDirectory())
            Files.createDirectories(destinationPath);
        else {
            Files.createDirectories(destinationPath.getParent());
            try (InputStream is = zipFile.getInputStream(entry)) {
                Files.copy(is, destinationPath);
            }
        }
    }

    public static void unzipSingle(String zipFileName, String contentFile, String destinationDir, String... unpackName) throws IOException {
        ZipFile zipFile = new ZipFile(zipFileName);
        ZipEntry entry = zipFile.getEntry(contentFile);
        try (InputStream is = zipFile.getInputStream(entry)) {
            IOUtils.copyStreamToFile(is,
                    IOUtils.pathCombine(destinationDir, (unpackName.length > 0) ? unpackName[0] : contentFile));
        }
    }

}
