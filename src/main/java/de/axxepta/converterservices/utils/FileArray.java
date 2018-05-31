package de.axxepta.converterservices.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 42kb on 21.07.2015.
 */
public class FileArray {

    private static String regexXmlDecl = "\\<\\?xml(.+?)\\?\\>";
    private static String regexEntity = "&(.*?);";         // regexEntity = "&([s|o].+?);";
    private static String replacementEntity = "<entityRef id=\"$1\"/>";

    // list of all File Lines
    private List<String> content = new ArrayList<>();

    public List<String> getContent() {
        return content;
    }

    public void add(String newLine) {
        content.add(newLine);
    }

    public int getSize() {
        return content.size();
    }

    public void clear() {
        content.clear();
    }

    // load file to an array line by line
    void load(String myFilePath) {
        String encoding = getXmlEncoding(myFilePath);
        try (FileInputStream fStream = new FileInputStream(myFilePath);
             BufferedReader br = new BufferedReader(new InputStreamReader(fStream, encoding))) {
            // Read File Line By Line
            String strLine;
            while ((strLine = br.readLine()) != null) {
                this.content.add(strLine);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String getXmlEncoding(String filePath) {
        try (FileInputStream fStream = new FileInputStream(filePath)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fStream));
            //Read File Line By Line
            String currentLine;
            currentLine = br.readLine();
            br.close();
            //Find encoding in XML Declaration
            String regex = "encoding=[\"|\'](.*?)[\"|']";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(currentLine);
            String preResult = "";
            String result;
            if (matcher.find()) {
                preResult = matcher.group(1);
            }
            switch (preResult.toLowerCase()) {
                case "utf-8"        :   result = "UTF-8"; break;
                case "iso-8859-1"   :   result = "ISO-8859-1"; break;
                default             :   {
                    result = "UTF-8";
                    System.out.println("Warning: Unknown file encoding ("+ preResult +") in file (" + filePath +
                            "); using default ---getXmlEncoding()");
                }
            }
            return result;
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Error while reading file encoding; using default ---getXmlEncoding()");
            return "UTF-8";
        }
    }



    // save array to file line by line (buffered)
    public void saveFileArray (String filePath) {
        try (FileOutputStream fStream = new FileOutputStream(filePath);
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fStream, "UTF8"));) {
            for (String line : content) {
                bw.write(line.concat("\r\n"));
            }
            bw.flush();
            fStream.flush();
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }

    // loads external entity and replace Entity-Ref by Element
    void wrapEntity (FileArray currentFileArray, Entities myEntities) {
        for (String line : currentFileArray.content) {
            String currentLine = line;

            //delete XML Declaration
            currentLine = currentLine.replaceAll(regexXmlDecl, "");

            // find and check entities
            Pattern patternEntity = Pattern.compile(regexEntity);
            Matcher matcherEntities = patternEntity.matcher(currentLine);
            if (matcherEntities.find()) {
                myEntities.chkEntities(matcherEntities.group(1));
            }

            // wrap entity
            currentLine = currentLine.replaceAll(regexEntity, replacementEntity);

            // delete fragment entity declaration
            currentLine = currentLine.replace("]>", "");

            //add replaced Line to new Content
            content.add(currentLine);
        }
    }

    // Add another FileArray at the end of the current
    public void addFileArray(FileArray fileArrayToAdd) {
        content.addAll(fileArrayToAdd.getContent());
    }

    // print the content to console
    public void printFileArray (){
        int i = 0;
        for (String contentItem : content) {
            System.out.println(i);
            System.out.println(contentItem);
        }
    }
}
