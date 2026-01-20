package com.gpoole.serialgui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Manages persistent configuration for the Serial Talker Logger application.
 * 
 * Stores and retrieves user settings to ~/.serialtalker/config.properties
 * Settings are automatically saved when changes are made.
 * 
 * Managed settings include:
 * - Serial port parameters (baud rate, data bits, stop bits, parity)
 * - Window dimensions and position
 * - Auto-negotiate speed preference
 * - Display mode (ASCII, HEX, HEX_AND_ASCII)
 * - Last used port
 */
public class ConfigurationManager {
    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".serialtalker");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    private final Properties properties;

    /**
     * Creates a new ConfigurationManager and loads existing settings.
     */
    public ConfigurationManager() {
        this.properties = new Properties();
        loadConfiguration();
    }

    /**
     * Loads configuration from file if it exists.
     */
    private void loadConfiguration() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                properties.load(Files.newInputStream(CONFIG_FILE));
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Saves configuration to file.
     */
    public void saveConfiguration() {
        try {
            Files.createDirectories(CONFIG_DIR);
            properties.store(Files.newOutputStream(CONFIG_FILE), "Serial Talker Logger Configuration");
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
        }
    }

    /**
     * Gets a configuration value with a default fallback.
     *
     * @param key The configuration key
     * @param defaultValue The default value if key not found
     * @return The configuration value or default
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Gets an integer configuration value.
     *
     * @param key The configuration key
     * @param defaultValue The default value if key not found
     * @return The configuration value or default
     */
    public int getInt(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a boolean configuration value.
     *
     * @param key The configuration key
     * @param defaultValue The default value if key not found
     * @return The configuration value or default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Sets a configuration value.
     *
     * @param key The configuration key
     * @param value The value to set
     */
    public void setString(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * Sets an integer configuration value.
     *
     * @param key The configuration key
     * @param value The value to set
     */
    public void setInt(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
    }

    /**
     * Sets a boolean configuration value.
     *
     * @param key The configuration key
     * @param value The value to set
     */
    public void setBoolean(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
    }

    // Configuration key constants
    public static final String KEY_BAUD_RATE = "serial.baudrate";
    public static final String KEY_DATA_BITS = "serial.databits";
    public static final String KEY_STOP_BITS = "serial.stopbits";
    public static final String KEY_PARITY = "serial.parity";
    public static final String KEY_LAST_PORT = "ui.lastport";
    public static final String KEY_WINDOW_WIDTH = "ui.window.width";
    public static final String KEY_WINDOW_HEIGHT = "ui.window.height";
    public static final String KEY_WINDOW_X = "ui.window.x";
    public static final String KEY_WINDOW_Y = "ui.window.y";
    public static final String KEY_AUTO_NEGOTIATE = "serial.autonegotiate";
    public static final String KEY_DISPLAY_MODE = "ui.displaymode";
}
