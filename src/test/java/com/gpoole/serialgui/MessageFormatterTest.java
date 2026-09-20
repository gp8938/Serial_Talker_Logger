package com.gpoole.serialgui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageFormatterTest {

    @Test
    void formatAsciiIncludesTimestampAndDirection() {
        MessageFormatter formatter = new MessageFormatter(MessageFormatter.DisplayMode.ASCII);
        String result = formatter.format("hello", false);
        assertTrue(result.contains("TX: hello"));
        assertTrue(result.startsWith("["));
    }

    @Test
    void formatHexConvertsToHex() {
        MessageFormatter formatter = new MessageFormatter(MessageFormatter.DisplayMode.HEX);
        String result = formatter.format("AB", true);
        assertTrue(result.contains("RX"));
        assertTrue(result.contains("41 42"));
    }

    @Test
    void formatHexAndAsciiShowsBoth() {
        MessageFormatter formatter = new MessageFormatter(MessageFormatter.DisplayMode.HEX_AND_ASCII);
        String result = formatter.format("AB", false);
        assertTrue(result.contains("41 42"));
        assertTrue(result.contains("(AB)"));
    }

    @Test
    void formatHandlesNull() {
        MessageFormatter formatter = new MessageFormatter(MessageFormatter.DisplayMode.ASCII);
        String result = formatter.format(null, true);
        assertTrue(result.contains("RX"));
    }

    @Test
    void formatEmptyString() {
        MessageFormatter formatter = new MessageFormatter(MessageFormatter.DisplayMode.HEX);
        String result = formatter.format("", false);
        assertTrue(result.contains("TX"));
    }
}