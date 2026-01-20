package com.gpoole.serialgui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles formatting of serial messages with timestamps and display mode conversion.
 * 
 * Features:
 * - Adds [HH:mm:ss.SSS] timestamp to each message
 * - Supports three display modes: ASCII (plain text), HEX (hex codes), HEX_AND_ASCII (both)
 * - Prefixes with TX (transmitted) or RX (received) indicator
 * 
 * Example output: "[12:34:56.789] RX: Hello World"
 */
public class MessageFormatter {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private DisplayMode displayMode;

    /**
     * Enumeration of supported message display modes.
     */
    public enum DisplayMode {
        /** Display as plain ASCII text */
        ASCII,
        /** Display as hexadecimal values */
        HEX,
        /** Display both ASCII and hexadecimal representations */
        HEX_AND_ASCII
    }

    /**
     * Creates a new MessageFormatter with the specified display mode.
     *
     * @param displayMode The initial display mode
     */
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
        this.displayMode = mode;
    }
}
