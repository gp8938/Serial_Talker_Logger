package com.gpoole.serialgui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages automatic logging of serial communication to file.
 * Supports log rotation by size or time.
 */
public class AutoLogManager {
    private static final Logger logger = LoggerFactory.getLogger(AutoLogManager.class);
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final long MAX_LOG_SIZE = 10 * 1024 * 1024; // 10MB

    private Path logFile;
    private boolean enabled = false;
    private boolean rotateOnSize = true;
    private long currentSize = 0;

    /**
     * Enables auto-logging to specified file.
     */
    public void enable(Path file) {
        this.logFile = file;
        this.enabled = true;
        this.currentSize = Files.exists(file) ? file.toFile().length() : 0;
        logger.info("Auto-log enabled: {}", file);
    }

    /**
     * Disables auto-logging.
     */
    public void disable() {
        this.enabled = false;
        logger.info("Auto-log disabled");
    }

    /**
     * Checks if auto-logging is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Logs a message to file with timestamp.
     */
    public void log(String direction, String message) {
        if (!enabled || logFile == null) return;

        try {
            String timestamp = LocalDateTime.now().format(FILE_FORMAT);
            String entry = String.format("[%s] %s: %s%n", timestamp, direction, message);
            byte[] data = entry.getBytes();

            if (rotateOnSize && currentSize + data.length > MAX_LOG_SIZE) {
                rotateLog();
            }

            Files.writeString(logFile, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            currentSize += data.length;
        } catch (IOException e) {
            logger.error("Failed to write to log file: {}", e.getMessage());
        }
    }

    /**
     * Rotates log file when size limit reached.
     */
    private void rotateLog() throws IOException {
        String timestamp = LocalDateTime.now().format(FILE_FORMAT);
        Path rotated = logFile.resolveSibling(logFile.getFileName() + "." + timestamp + ".old");
        Files.move(logFile, rotated);
        currentSize = 0;
        logger.info("Log rotated to: {}", rotated);
    }

    /**
     * Gets current log file path.
     */
    public Path getLogFile() {
        return logFile;
    }
}
