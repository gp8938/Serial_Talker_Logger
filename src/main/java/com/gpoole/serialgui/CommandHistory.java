package com.gpoole.serialgui;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages command history for the serial communication input field.
 * Allows navigating through previously entered commands using up/down keys.
 */
public class CommandHistory {
    private final List<String> history;
    private int currentIndex;
    private static final int MAX_HISTORY = 50;

    public CommandHistory() {
        this.history = new ArrayList<>();
        this.currentIndex = -1;
    }

    /**
     * Adds a command to the history.
     *
     * @param command The command to add
     */
    public void add(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        // Don't add duplicates at the end
        if (!history.isEmpty() && history.get(history.size() - 1).equals(command)) {
            return;
        }
        history.add(command);
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
        currentIndex = -1; // Reset position after adding
    }

    /**
     * Gets the previous command in history.
     *
     * @return The previous command, or empty string if at the beginning
     */
    public String getPrevious() {
        if (history.isEmpty()) {
            return "";
        }
        if (currentIndex < 0) {
            currentIndex = history.size() - 1;
        } else if (currentIndex > 0) {
            currentIndex--;
        }
        return currentIndex >= 0 && currentIndex < history.size()
            ? history.get(currentIndex)
            : "";
    }

    /**
     * Gets the next command in history.
     *
     * @return The next command, or empty string if at the end
     */
    public String getNext() {
        if (history.isEmpty()) {
            return "";
        }
        if (currentIndex >= 0 && currentIndex < history.size() - 1) {
            currentIndex++;
            return history.get(currentIndex);
        } else if (currentIndex == history.size() - 1) {
            currentIndex = -1;
            return "";
        }
        return "";
    }

    /**
     * Resets the history position.
     */
    public void reset() {
        currentIndex = -1;
    }

    /**
     * Gets the total number of commands in history.
     *
     * @return History size
     */
    public int size() {
        return history.size();
    }

    /**
     * Clears all history.
     */
    public void clear() {
        history.clear();
        currentIndex = -1;
    }
}
