package com.gpoole.serialgui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AutoLogManagerTest {

    @Test
    void enableAndLogWritesEntry() throws IOException {
        Path temp = Files.createTempFile("test-log", ".log");
        AutoLogManager manager = new AutoLogManager();
        manager.enable(temp);
        manager.log("TX", "hello");
        String content = Files.readString(temp);
        assertTrue(content.contains("TX"));
        assertTrue(content.contains("hello"));
    }

    @Test
    void disablePreventsLogging() throws IOException {
        Path temp = Files.createTempFile("test-log", ".log");
        Files.writeString(temp, "existing");
        AutoLogManager manager = new AutoLogManager();
        manager.enable(temp);
        manager.disable();
        manager.log("RX", "should-not-appear");
        String content = Files.readString(temp);
        assertFalse(content.contains("should-not-appear"));
    }

    @Test
    void logWithoutEnabledDoesNothing() throws IOException {
        Path temp = Files.createTempFile("test-log", ".log");
        AutoLogManager manager = new AutoLogManager();
        manager.log("TX", "nothing");
        assertEquals(0, Files.size(temp));
    }
}