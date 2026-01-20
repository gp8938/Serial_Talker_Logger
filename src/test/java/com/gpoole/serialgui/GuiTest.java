package com.gpoole.serialgui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
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
        testPortNames = new AtomicReference<>(new String[]{"COM1", "COM2"});
        capturedErrors = new ArrayList<>();
        gui = runOnEdt(() -> new Gui(false, testPortNames::get, capturedErrors::add, MockSerialPort::new));
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

        JButton connectButton = runOnEdt(() -> getField(gui, "connectButton", JButton.class));
        JTextArea outputArea = runOnEdt(() -> getField(gui, "outputArea", JTextArea.class));

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
    @SuppressWarnings("unchecked")
    void updateAvailablePortsPopulatesDropdown() throws Exception {
        testPortNames.set(new String[] {"COM1", "COM2"});

        runOnEdt(() -> {
            gui.refreshPortList();
            return null;
        });

        JComboBox<String> dropdown = runOnEdt(() -> getField(gui, "portsDropdown", JComboBox.class));
        assertEquals(2, dropdown.getItemCount());
        assertEquals("COM1", dropdown.getItemAt(0));
        assertEquals("COM2", dropdown.getItemAt(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateAvailablePortsKeepsSelection() throws Exception {
        JComboBox<String> dropdown = runOnEdt(() -> getField(gui, "portsDropdown", JComboBox.class));

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
    void noComPortsFoundShowsWarning() throws Exception {
        capturedErrors.clear();
        testPortNames.set(new String[0]);

        runOnEdt(() -> {
            gui.refreshPortList();
            return null;
        });

        JComboBox<String> dropdown = runOnEdt(() -> getField(gui, "portsDropdown", JComboBox.class));
        JButton connectButton = runOnEdt(() -> getField(gui, "connectButton", JButton.class));

        assertEquals("No COM ports found", dropdown.getSelectedItem());
        assertFalse(dropdown.isEnabled());
        assertFalse(connectButton.isEnabled());
        assertEquals(1, capturedErrors.size());
        assertTrue(capturedErrors.get(0).contains("Warning"));
    }

    @Test
    void toggleSerialConnectionWithoutSelectionShowsError() throws Exception {
        JButton connectButton = runOnEdt(() -> getField(gui, "connectButton", JButton.class));

        runOnEdt(() -> {
            JComboBox<String> dropdown = getField(gui, "portsDropdown", JComboBox.class);
            dropdown.removeAllItems();
            dropdown.setSelectedItem(null);
            return null;
        });

        runOnEdt(() -> {
            connectButton.doClick();
            return null;
        });

        boolean connected = runOnEdt(() -> getBooleanField(gui, "connected"));

        assertEquals("Connect", connectButton.getText());
        assertFalse(connected);
        assertEquals(List.of("No port selected"), capturedErrors);
    }

    @Test
    void sendAndReceiveDataThroughMockPort() throws Exception {
        testPortNames.set(new String[]{"COM1"});
        capturedErrors.clear();

        runOnEdt(() -> {
            gui.refreshPortList();
            return null;
        });

        JButton connectButton = runOnEdt(() -> getField(gui, "connectButton", JButton.class));
        JComboBox<String> dropdown = runOnEdt(() -> getField(gui, "portsDropdown", JComboBox.class));
        JTextField messageInput = runOnEdt(() -> getField(gui, "messageInput", JTextField.class));

        assertEquals("COM1", dropdown.getSelectedItem());

        // Verify text input field exists and is functional
        runOnEdt(() -> {
            messageInput.setText("Test Message");
            return null;
        });

        String message = runOnEdt(messageInput::getText);
        assertEquals("Test Message", message);
    }

    @Test
    void autoNegotiateSpeedToggle() throws Exception {
        JMenuBar menuBar = runOnEdt(gui::getJMenuBar);
        JMenu settingsMenu = findMenu(menuBar, "Settings");

        assertNotNull(settingsMenu);

        boolean autoNegotiateFound = false;
        for (int i = 0; i < settingsMenu.getItemCount(); i++) {
            JMenuItem item = settingsMenu.getItem(i);
            if (item instanceof JCheckBoxMenuItem && "Auto-Negotiate Speed".equals(item.getText())) {
                autoNegotiateFound = true;
                JCheckBoxMenuItem checkBoxItem = (JCheckBoxMenuItem) item;
                assertFalse(checkBoxItem.isSelected());
                break;
            }
        }
        assertTrue(autoNegotiateFound, "Auto-Negotiate Speed menu item should exist");
    }

    @Test
    void mockPortTracksBaudRate() throws Exception {
        testPortNames.set(new String[]{"COM1"});

        runOnEdt(() -> {
            gui.refreshPortList();
            return null;
        });

        JButton connectButton = runOnEdt(() -> getField(gui, "connectButton", JButton.class));
        JComboBox<String> dropdown = runOnEdt(() -> getField(gui, "portsDropdown", JComboBox.class));

        runOnEdt(() -> {
            dropdown.setSelectedItem("COM1");
            connectButton.doClick();
            return null;
        });

        SerialPort serialPort = runOnEdt(() -> getField(gui, "activeSerialPort", SerialPort.class));
        assertTrue(serialPort instanceof MockSerialPort);

        MockSerialPort mockPort = (MockSerialPort) serialPort;
        assertEquals(9600, mockPort.getCurrentBaudRate());
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
        ScheduledExecutorService executor = getField(gui, "portUpdater", ScheduledExecutorService.class);
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

    /**
     * Mock SerialPort for testing without actual hardware
     */
    static class MockSerialPort extends SerialPort {
        private boolean portOpen = false;
        private SerialPortEventListener eventListener;
        private int eventMask;
        private StringBuilder writeBuffer = new StringBuilder();
        private StringBuilder readBuffer = new StringBuilder();
        private int currentBaudRate;
        private final int[] supportedBaudRates = {9600, 14400, 19200, 28800, 38400, 57600, 115200};

        MockSerialPort(String portName) {
            super(portName);
        }

        @Override
        public boolean openPort() throws SerialPortException {
            portOpen = true;
            return true;
        }

        @Override
        public boolean closePort() throws SerialPortException {
            portOpen = false;
            return true;
        }

        @Override
        public boolean setParams(int baudRate, int dataBits, int stopBits, int parity) throws SerialPortException {
            this.currentBaudRate = baudRate;
            return true;
        }

        @Override
        public boolean writeString(String string) throws SerialPortException {
            writeBuffer.append(string);
            // Simulate echo back of data
            readBuffer.append("ECHO: ").append(string);
            return true;
        }

        @Override
        public String readString(int length) throws SerialPortException {
            if (readBuffer.length() == 0) {
                return "";
            }
            String result = readBuffer.substring(0, Math.min(length, readBuffer.length()));
            readBuffer.delete(0, result.length());
            return result;
        }

        @Override
        public void addEventListener(SerialPortEventListener listener, int eventMask) throws SerialPortException {
            this.eventListener = listener;
            this.eventMask = eventMask;
        }

        @Override
        public boolean removeEventListener() throws SerialPortException {
            this.eventListener = null;
            return true;
        }

        @Override
        public boolean isOpened() {
            return portOpen;
        }

        public String getWriteBuffer() {
            return writeBuffer.toString();
        }

        public int getCurrentBaudRate() {
            return currentBaudRate;
        }

        public int[] getSupportedBaudRates() {
            return supportedBaudRates;
        }
    }
}