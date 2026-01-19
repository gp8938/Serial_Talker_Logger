package com.gpoole.serialgui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import javax.swing.*;
import java.awt.*;

class GuiTest {
    private Gui gui;
    
    @BeforeEach
    void setUp() {
        // Only create GUI if not in headless mode
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipping test in headless environment");
        gui = new Gui();
    }
    
    @AfterEach
    void tearDown() {
        if (gui != null) {
            gui.dispose();
        }
    }
    
    @Test
    void testInitialState() {
        assertNotNull(gui, "GUI should be initialized");
        assertEquals("Serial Communication GUI", gui.getTitle(), "Window title should match");
        assertEquals(new Dimension(800, 600), gui.getSize(), "Window size should match");
    }
    
    @Test
    void testPortSelection() {
        JComboBox<?> portList = findComponentByType(gui, JComboBox.class);
        assertNotNull(portList, "Port list should exist");
    }
    
    @Test
    void testConnectButtonInitialState() {
        JButton connectButton = findButtonByText(gui, "Connect");
        assertNotNull(connectButton, "Connect button should exist");
        assertEquals("Connect", connectButton.getText(), "Initial button text should be 'Connect'");
    }
    
    @Test
    void testOutputAreaExists() {
        JTextArea outputArea = findComponentByType(gui, JTextArea.class);
        assertNotNull(outputArea, "Output area should exist");
        assertFalse(outputArea.isEditable(), "Output area should not be editable");
    }
    
    @Test
    void testSettingsDialogComponents() {
        JMenuBar menuBar = gui.getJMenuBar();
        JMenu settingsMenu = findMenuByText(menuBar, "Settings");
        assertNotNull(settingsMenu, "Settings menu should exist");
        
        JMenuItem settingsItem = findMenuItemByText(settingsMenu, "Settings");
        assertNotNull(settingsItem, "Settings menu item should exist");
    }
    
    // Helper methods
    private <T> T findComponentByType(Container container, Class<T> type) {
        if (type.isInstance(container)) {
            return type.cast(container);
        }
        
        for (Component comp : container.getComponents()) {
            if (type.isInstance(comp)) {
                return type.cast(comp);
            }
            if (comp instanceof Container container1) {
                T found = findComponentByType(container1, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    private JButton findButtonByText(Container container, String text) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (comp instanceof Container container1) {
                JButton found = findButtonByText(container1, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    private JMenu findMenuByText(JMenuBar menuBar, String text) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (text.equals(menu.getText())) {
                return menu;
            }
        }
        return null;
    }
    
    private JMenuItem findMenuItemByText(JMenu menu, String text) {
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null && text.equals(item.getText())) {
                return item;
            }
        }
        return null;
    }
}