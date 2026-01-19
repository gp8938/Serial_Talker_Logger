package com.gpoole.serialgui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GuiTest {
    private Gui gui;
    private AtomicReference<String[]> testPortNames;
    private List<String> capturedErrors;

    @BeforeEach
    void setUp() throws Exception {
        testPortNames = new AtomicReference<>(new String[0]);
        capturedErrors = new ArrayList<>();
        gui = runOnEdt(() -> new Gui(false, testPortNames::get, capturedErrors::add));
        shutdownPortUpdater(gui);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (gui != null) {
            shutdownPortUpdater(gui);
            runOnEdt(() -> {
                gui.dispose();
                return null;
            });
        }
    }

    @Test
    void initialFrameSetup() throws Exception {
        assertEquals("Serial Communication GUI", runOnEdt(gui::getTitle));
        assertEquals(new Dimension(800, 600), runOnEdt(gui::getSize));

        JButton connectButton = runOnEdt(() -> getField(gui, "connectionToggleButton", JButton.class));
        JTextArea outputArea = runOnEdt(() -> getField(gui, "serialOutputArea", JTextArea.class));

        assertEquals("Connect", connectButton.getText());
        assertFalse(outputArea.isEditable());
    }

    @Test
    void menuContainsFileAndSettings() throws Exception {
        JMenuBar menuBar = runOnEdt(gui::getJMenuBar);
        JMenu fileMenu = findMenu(menuBar, "File");
        JMenu settingsMenu = findMenu(menuBar, "Settings");

        assertNotNull(fileMenu, "File menu should exist");
        assertNotNull(settingsMenu, "Settings menu should exist");

        JMenuItem saveItem = findMenuItem(fileMenu, "Save");
        JMenuItem exitItem = findMenuItem(fileMenu, "Exit");
        JMenuItem settingsItem = findMenuItem(settingsMenu, "Settings");

        assertNotNull(saveItem, "Save item should exist");
        assertNotNull(exitItem, "Exit item should exist");
        assertNotNull(settingsItem, "Settings item should exist");
    }

    @Test
    void updateAvailablePortsPopulatesDropdown() throws Exception {
        testPortNames.set(new String[] {"COM1", "COM2"});

        runOnEdt(() -> {
            gui.refreshPortList();
            return null;
        });

        JComboBox<String> dropdown = runOnEdt(() -> getField(gui, "availablePortsDropdown", JComboBox.class));
        assertEquals(2, dropdown.getItemCount());
        assertEquals("COM1", dropdown.getItemAt(0));
        assertEquals("COM2", dropdown.getItemAt(1));
    }

    @Test
    void updateAvailablePortsKeepsSelection() throws Exception {
        JComboBox<String> dropdown = runOnEdt(() -> getField(gui, "availablePortsDropdown", JComboBox.class));

        runOnEdt(() -> {
            dropdown.addItem("COM3");
            dropdown.addItem("COM4");
            dropdown.setSelectedItem("COM4");
            return null;
        });

        testPortNames.set(new String[] {"COM1", "COM4", "COM5"});

        runOnEdt(() -> {
            gui.refreshPortList();
            return null;
        });

        assertEquals("COM4", dropdown.getSelectedItem());
        assertEquals(3, dropdown.getItemCount());
    }

    @Test
    void toggleSerialConnectionWithoutSelectionShowsError() throws Exception {
        JButton connectButton = runOnEdt(() -> getField(gui, "connectionToggleButton", JButton.class));

        runOnEdt(() -> {
            JComboBox<String> dropdown = getField(gui, "availablePortsDropdown", JComboBox.class);
            dropdown.removeAllItems();
            dropdown.setSelectedItem(null);
            return null;
        });

        runOnEdt(() -> {
            connectButton.doClick();
            return null;
        });

        boolean connected = runOnEdt(() -> getBooleanField(gui, "isSerialPortConnected"));

        assertEquals("Connect", connectButton.getText());
        assertFalse(connected);
        assertEquals(List.of("No port selected"), capturedErrors);
    }

    private static JMenu findMenu(JMenuBar menuBar, String text) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null && text.equals(menu.getText())) {
                return menu;
            }
        }
        return null;
    }

    private static JMenuItem findMenuItem(JMenu menu, String text) {
        if (menu == null) {
            return null;
        }

        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null && text.equals(item.getText())) {
                return item;
            }
        }
        return null;
    }

    private static void shutdownPortUpdater(Gui gui) throws Exception {
        ScheduledExecutorService executor = getField(gui, "portListUpdater", ScheduledExecutorService.class);
        executor.shutdownNow();
    }

    private static boolean getBooleanField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private static <T> T getField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static <T> T runOnEdt(Callable<T> task) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return task.call();
        }

        FutureTask<T> future = new FutureTask<>(task);
        SwingUtilities.invokeAndWait(future);
        return future.get();
    }
}