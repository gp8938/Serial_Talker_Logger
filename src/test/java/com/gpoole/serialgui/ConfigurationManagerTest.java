package com.gpoole.serialgui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationManagerTest {

    @Test
    void getAndSetRoundTrip() {
        ConfigurationManager config = new ConfigurationManager();
        config.setString("test.key", "value");
        assertEquals("value", config.getString("test.key", "default"));
    }

    @Test
    void getIntReturnsDefaultOnMissing() {
        ConfigurationManager config = new ConfigurationManager();
        assertEquals(42, config.getInt("missing.key", 42));
    }

    @Test
    void getIntParsesValidValue() {
        ConfigurationManager config = new ConfigurationManager();
        config.setString("number.key", "123");
        assertEquals(123, config.getInt("number.key", 0));
    }
}