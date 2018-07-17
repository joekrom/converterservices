package de.axxepta.converterservices;

import spark.servlet.SparkApplication;

public class ConverterSparkApplication implements SparkApplication {

    private static final String RELATIVE_PATH              = "/spark";

    @Override
    public void init() {
        App.init(RELATIVE_PATH);
    }
}
