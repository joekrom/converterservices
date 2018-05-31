package de.axxepta.converterservices.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 42kb on 23.07.2015.
 */
public class Entity {

    private List<String> names = new ArrayList<>();
    private String file = "";
    private String metaFile = "";
    private String value = "";
    private Boolean added = false;


    String getFile() {
        return file;
    }

    void setFile(String file) {
        this.file = file;
    }

    String getMetaFile() {
        return metaFile;
    }

    void setMetaFile(String metaFile) {
        this.metaFile = metaFile;
    }

    String getValue() {
        return value;
    }

    Boolean isAdded() {
        return added;
    }

    void setAdded(Boolean added) {
        this.added = added;
    }

    String getName(int index) {
        return names.get(index);
    }

    void addName(String name) {
        names.add(name);
    }

    int indexOfName(String name) {
        return name.indexOf(name);
    }

    String getNames() {
        String result = "";
        for (String name : names) {
            if (result.length() > 1)
                result = result + "###";
            result = result + name;
        }
        return result;
    }

}
