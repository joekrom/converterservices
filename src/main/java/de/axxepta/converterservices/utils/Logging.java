package de.axxepta.converterservices.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * This class is designed to be used as logging wrapper for XQuery Java Binding or
 */
public class Logging {

    private static final Logger LOGGER = LoggerFactory.getLogger(Logging.class);

    public static final String CLIENT = "client";

    public static final String TRACE = "TRACE";
    public static final String DEBUG = "DEBUG";
    public static final String INFO = "INFO";
    public static final String WARN = "WARN";
    public static final String ERROR = "ERROR";
    public static final String FATAL = "FATAL";
    public static final String NONE = "NONE";

    /**
     * Logging wrapper for XQuery Java binding
     * @param level One of the defined logger level constants
     * @param msg Message
     */
    public static void log(String level, String msg) {
        if ( (LOGGER != null && level != null) && !level.equals(NONE)) {
            MDC.put(CLIENT, IOUtils.getHostName());
            switch (level.toUpperCase()) {
                case TRACE:
                    LOGGER.trace(msg);
                    break;
                case DEBUG:
                    LOGGER.debug(msg);
                    break;
                case INFO:
                    LOGGER.info(msg);
                    break;
                case WARN:
                    LOGGER.warn(msg);
                    break;
                case ERROR:
                    LOGGER.error(msg);
                    break;
                default:
            }
            MDC.remove(CLIENT);
        }
    }

    /**
     * Logging wrapper for code/script defined logging level (e.g. in pipeline)
     * @param logger Logger of the calling class
     * @param level One of the defined logger level constants
     * @param msg Message
     */
    public static void log(Logger logger, String level, String msg) {
        if (logger == null) {
            logger = LOGGER;
        }
        if ( (logger != null && level != null) && !level.equals(NONE)) {
            MDC.put(CLIENT, IOUtils.getHostName());
            switch (level.toUpperCase()) {
                case TRACE:
                    logger.trace(msg);
                    break;
                case DEBUG:
                    logger.debug(msg);
                    break;
                case INFO:
                    logger.info(msg);
                    break;
                case WARN:
                    logger.warn(msg);
                    break;
                case ERROR:
                    logger.error(msg);
                    break;
                default:
            }
            MDC.remove(CLIENT);
        }
    }

}
