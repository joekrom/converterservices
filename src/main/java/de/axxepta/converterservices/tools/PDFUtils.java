package de.axxepta.converterservices.tools;

import de.axxepta.converterservices.utils.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFUtils {

    private static Logger logger = LoggerFactory.getLogger(PDFUtils.class);

    private PDFUtils() {}

    private static List<String> splitPDF(String sourcePath, String targetPath, boolean imageOutput, List<Integer> widthList, List<Integer> heightList) {
        List<String> pdfOutputList = new ArrayList<>();
        Integer pageNumber = 0;
        try {
            File pdfFile = new File(sourcePath);
            if (pdfFile.exists()) {
                String fileName = targetPath.replace(".pdf", "");
                String pathName;
                String targetDir = IOUtils.dirFromPath(targetPath);
                IOUtils.safeCreateDirectory(targetDir);
                PDDocument document = PDDocument.load(pdfFile);
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                PDPageTree pages = document.getPages();
                for (PDPage page : pages) {
                    pageNumber++;
                    System.out.println(pageNumber - 1);
                    if (imageOutput) {
                        BufferedImage bim = pdfRenderer.renderImageWithDPI(pageNumber - 1, 300, ImageType.RGB);
                        pathName = fileName + "_" + pageNumber + ".png";
                        widthList.add(bim.getWidth());
                        heightList.add(bim.getHeight());
                        ImageIOUtil.writeImage(bim, pathName, 300);
                    } else {
                        PDDocument newDocument = new PDDocument();
                        newDocument.addPage(page);
                        pathName = fileName + "_" + pageNumber + ".pdf";
                        File newFile = new File(pathName);
                        if (!newFile.createNewFile()) {
                            logger.error("Exception splitting PDF resource: File " + pathName + " could not be created.");
                        }
                        newDocument.save(newFile);
                        newDocument.close();
                    }
                    pdfOutputList.add(IOUtils.filenameFromPath(pathName));
                }
                document.close();
            } else {
                logger.error("Input file for splitting not found: " + sourcePath);
            }
        } catch (IOException ex) {
            logger.error("Exception splitting PDF resource: " + ex.getMessage());
        }
        return pdfOutputList;
    }

    public static List<String> splitPDF(String inputFile, boolean convertToPNG, String path) throws IOException, InterruptedException {
        List<Integer> wList = new ArrayList<>();
        List<Integer> hList = new ArrayList<>();
        return splitPDF(path + File.separator + inputFile, path + File.separator + inputFile, convertToPNG, wList, hList);
    }
}
