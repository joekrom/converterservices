package de.axxepta.converterservices.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 42kb on 23.07.2015.
 */
public class Entities {

    private List<Entity> entries = new ArrayList<>();

    void addEntry(Entity entity) {
        entries.add(entity);
    }

    // get EntityDeclarations by a filename and find the start Entity
    public String getEntityDeclByFileName (String fileName) {
        //Load File to Lines
        String myStartEntity = "";
        FileArray in = new FileArray();
        in.load(fileName);
        // Process FileArray Line by Line and find EntityDeclarations
        for (String currentLine : in.getContent()) {
            int isEntity = currentLine.indexOf("<!ENTITY");
            if (isEntity != -1) {
                String[] parts = currentLine.split(" ");
                Entity entity = new Entity();
                entity.addName(parts[1]);
                entity.setFile(parts[3].substring(1, parts[3].length() - 2));
                this.entries.add(entity);
            }
            //Start entity find and store
            String regexEntity = "&(.*?);";
            Pattern patternEntity = Pattern.compile(regexEntity);
            Matcher matcherEntities = patternEntity.matcher(currentLine);
            if (matcherEntities.find()) {
                myStartEntity = matcherEntities.group(1);
            }
        }

        if (myStartEntity.length() > 1){
            System.out.println("StartEntity=" + myStartEntity + " ---getEntityDeclByFileName()");
        }
        else {
            System.out.println("Error: No StartEntity found! ---getEntityDeclByFileName()");
        }
        return myStartEntity;
    }


    // normalize EntityList by filename (find duplicates)
    public Entities normalize() {
        Entities newEntities = new Entities();
        List<String> refList = new ArrayList<>();
        for (Entity entity : entries) {
            String currentFile = entity.getFile();
            if (refList.indexOf(currentFile) != -1) {
                //entry present
                newEntities.getEntityByFileName(currentFile).addName(entity.getName(0));
            } else {
                //new entry
                refList.add(currentFile);
                newEntities.addEntry(entity);
            }
        }
        return newEntities;
    }


    // add EntityName if not in list
    void chkEntities(String myEntityName) {
        for (Entity entity : entries) {
            if (entity.indexOfName(myEntityName) != -1) {
                return;
            }
        }
        Entity newEntity = new Entity();
        newEntity.addName(myEntityName);
        newEntity.setFile("");
        newEntity.setAdded(true);
        entries.add(newEntity);
    }

    Entity getEntityByFileName (String myFileName) {
        for (Entity entity : entries) {
            if (entity.getFile().equals(myFileName)) {
                return entity;
            }
        }
        return null;
    }

    void addMetaFileByFileName (String myEntityFile, String myMetaFile) {
        for (Entity entity : entries) {
            if (myEntityFile.equals(entity.getFile())) {
                entity.setMetaFile(myMetaFile);
            }
        }
    }

    public FileArray toCsv () {
        int i = 1;
        FileArray csv = new FileArray();
        csv.add("count;Name;file;metaFile;value;added");
        for (Entity entity : entries) {
            StringJoiner joiner = new StringJoiner(";");
            csv.add(i + ";" + entity.getNames() + ";" + entity.getFile() + ";" +
                    entity.getMetaFile() + ";" + entity.getValue() + ";" + entity.isAdded());
            i++;
        }
        return csv;
    }

    // print values to console
    public void debug () {
        int i = 0;
        System.out.println("START debug Entities");
        for (Entity entity : entries) {
            System.out.println(i + " ## name=" + entity.getNames() + " ## metaFile=" + entity.getMetaFile() +
                    " ## fileName=" + entity.getFile() + " ## value=" + entity.getValue() +
                    " ## added=" + entity.isAdded());
            i++;
        }
        System.out.println("END debug Entities");
    }
}
