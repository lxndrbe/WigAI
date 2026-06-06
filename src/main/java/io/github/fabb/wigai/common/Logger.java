package io.github.fabb.wigai.common;

import com.bitwig.extension.controller.api.ControllerHost;
import java.io.FileWriter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logger implementation for the WigAI extension.
 * Uses Bitwig's ControllerHost.println and writes to a debug log file on disk.
 */
public class Logger {
    private final ControllerHost host;
    private static final String LOG_FILE_PATH = "C:\\Users\\lxndr\\Documents\\CLAUDE\\Bitwig-Claude\\WigAI2\\wigai_debug.log";

    /**
     * Creates a new Logger instance.
     *
     * @param host The Bitwig ControllerHost to use for logging
     */
    public Logger(ControllerHost host) {
        if (host == null) {
            throw new IllegalArgumentException("ControllerHost cannot be null");
        }
        this.host = host;
        logToFile("INFO", "Logger initialized");
    }

    private void logToFile(String level, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        String line = "[" + timestamp + "] [" + level + "] " + message + "\n";
        try {
            File file = new File(LOG_FILE_PATH);
            try (FileWriter fw = new FileWriter(file, true)) {
                fw.write(line);
            }
        } catch (Exception e) {
            // Fallback if writing fails
            host.println("[ERROR] Failed to write to log file: " + e.getMessage());
        }
    }

    /**
     * Log an informational message.
     *
     * @param message The message to log
     */
    public void info(String message) {
        host.println("[INFO] " + message);
        logToFile("INFO", message);
    }

    /**
     * Log a warning message.
     *
     * @param message The message to log
     */
    public void warn(String message) {
        host.println("[WARN] " + message);
        logToFile("WARN", message);
    }

    /**
     * Log an error message.
     *
     * @param message The message to log
     */
    public void error(String message) {
        host.println("[ERROR] " + message);
        logToFile("ERROR", message);
    }

    /**
     * Log a debug message.
     *
     * @param message The message to log
     */
    public void debug(String message) {
        host.println("[DEBUG] " + message);
        logToFile("DEBUG", message);
    }

    /**
     * Log an exception with an error message.
     *
     * @param message The error message
     * @param e       The exception to log
     */
    public void error(String message, Throwable e) {
        host.println("[ERROR] " + message + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
        logToFile("ERROR", message + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
        
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            host.println("    at " + element.toString());
            sb.append("    at ").append(element.toString()).append("\n");
        }
        logToFile("STACKTRACE", sb.toString());
    }
}
