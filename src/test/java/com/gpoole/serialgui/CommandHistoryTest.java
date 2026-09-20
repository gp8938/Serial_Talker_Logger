package com.gpoole.serialgui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandHistoryTest {

    private CommandHistory history;

    @BeforeEach
    void setUp() {
        history = new CommandHistory();
    }

    @Test
    void addAndNavigate() {
        history.add("first");
        history.add("second");
        assertEquals("second", history.getPrevious());
        assertEquals("first", history.getPrevious());
        assertEquals("", history.getPrevious());
    }

    @Test
    void duplicatePrevented() {
        history.add("cmd");
        history.add("cmd");
        assertEquals("cmd", history.getPrevious());
        assertEquals("", history.getPrevious());
    }

    @Test
    void emptyHistoryReturnsEmpty() {
        assertEquals("", history.getPrevious());
        assertEquals("", history.getNext());
    }

    @Test
    void navigationResetAfterAdd() {
        history.add("one");
        history.add("two");
        history.getPrevious(); // position 1
        history.add("three");
        assertEquals("three", history.getPrevious());
    }

    @Test
    void nullOrBlankIgnored() {
        history.add(null);
        history.add("   ");
        assertTrue(history.getPrevious().isEmpty());
    }
}