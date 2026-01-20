package com.gpoole.serialgui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles formatting of serial messages with timestamps and display mode conversion.
 */
public class MessageFormatter {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final DisplayMode displayMode;

    public enum DisplayMode {
        ASCII,
        HEX,
        HEX_AND_ASCII
    }

    public MessageFormatter(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    /**
     * Formats a message with timestamp and converts to appropriate display format.
     *
     * @param message The message to format
     * @param isReceived true if message was received, false if sent
     * @return Formatted message with timestamp and display mode conversion
     */
    public String format(String message, boolean isReceived) {
        String timestamp = "[" + LocalTime.now().format(TIME_FORMAT) + "]";
        String direction = isReceived ? "RX" : "TX";
        String formattedMessage = convertToDisplayMode(message);
        return timestamp + " " + direction + ": " + formattedMessage;
    }

    /**
     * Converts message to the current display mode.
     *
     * @param message The message to convert
     * @return Message in current display mode
     */
    private String convertToDisplayMode(String message) {
        return switch (displayMode) {
            case ASCII -> message;
            case HEX -> stringToHex(message);
            case HEX_AND_ASCII -> stringToHex(message) + " (" + message + ")";
        };
    }

    /**
     * Converts a string to its hexadecimal representation.
     *
     * @param input The string to convert
     * @return Hexadecimal representation
     */
    private String stringToHex(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder hex = new StringBuilder();
        for (char c : input.toCharArray()) {
            hex.append(String.format("%02X ", (int) c));
        }
        return hex.toString().trim();
    }

    /**
     * Changes the display mode.
     *
     * @param mode The new display mode
     */
    public void setDisplayMode(DisplayMode mode) {
        // Note: Current implementation is immutable. For mutable version, would need to track state.
    }
}
