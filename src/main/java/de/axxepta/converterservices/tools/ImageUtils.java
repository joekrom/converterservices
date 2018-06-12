package de.axxepta.converterservices.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static ByteArrayOutputStream thumbify(String scaling, String size, String path) throws IOException, InterruptedException {
        return CmdUtils.runExternal("mogrify",
                "-flatten", "-strip",
                "-format", "jpg",
                "-quality", "75",
                "-thumbnail", scaling,
                "-gravity", "center", "-extent", size,  path);
    }


    public static String jpgFilename(String name) {
        int pos = name.lastIndexOf(".");
        return name.substring(0, (pos == -1) ? name.length() : pos) + ".jpg";
    }
}
