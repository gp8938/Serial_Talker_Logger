package com.gpoole.serialgui;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import jssc.SerialPortList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Gui extends JFrame {
    private final JTextArea serialOutputArea;
    private final JComboBox<String> availablePortsDropdown;
    private final JButton connectionToggleButton;
    private SerialPort activeSerialPort;
    private boolean isSerialPortConnected = false;
    private int selectedBaudRate = 9600;
    private int selectedDataBits = SerialPort.DATABITS_8;
    private int selectedStopBits = SerialPort.STOPBITS_1;
    private int selectedParity = SerialPort.PARITY_NONE;
    private final ScheduledExecutorService portListUpdater;
    private final Supplier<String[]> portNamesProvider;
    private final Consumer<String> errorHandler;

    public Gui() {
        this(true, SerialPortList::getPortNames, null);
    }

    Gui(boolean startPortUpdater) {
        this(startPortUpdater, SerialPortList::getPortNames, null);
    }

    Gui(boolean startPortUpdater, Supplier<String[]> portNamesProvider, Consumer<String> errorHandler) {
        this.portNamesProvider = portNamesProvider;
        this.errorHandler = errorHandler != null
            ? errorHandler
            : msg -> JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        portListUpdater = Executors.newScheduledThreadPool(1);
        setupFrame();
        
        // Create main components
        serialOutputArea = new JTextArea(15, 40);
        availablePortsDropdown = new JComboBox<>();
        connectionToggleButton = new JButton("Connect");
        
        setupMenuBar();
        setupMainPanel();
        setupControlPanel();
        
        // Start port list updater
        if (startPortUpdater) {
            portListUpdater.scheduleAtFixedRate(this::updateAvailablePorts, 0, 2, TimeUnit.SECONDS);
        }
        
        // Add window closing handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectSerialPort();
                portListUpdater.shutdown();
                System.exit(0);
            }
        });
    }

    private void setupFrame() {
        setTitle("Serial Communication GUI");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void setupMenuBar() {
        var menuBar = new JMenuBar();
        
        // File Menu
        var fileMenu = new JMenu("File");
        var saveMenuItem = new JMenuItem("Save");
        saveMenuItem.addActionListener(e -> saveOutputToFile());
        var exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> System.exit(0));
        fileMenu.add(saveMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        
        // Settings Menu
        var settingsMenu = new JMenu("Settings");
        var settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(e -> showSettingsDialog());
        settingsMenu.add(settingsItem);
        
        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);
    }

    private void setupMainPanel() {
        var mainPanel = new JPanel(new BorderLayout());
        
        // Output area with scroll
        serialOutputArea.setEditable(false);
        var scrollPane = new JScrollPane(serialOutputArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel on the right
        var buttonPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        var sendHelloWorldButton = new JButton("Hello World");
        sendHelloWorldButton.addActionListener(e -> sendSerialMessage("Hello World!"));
        
        var sendLongListButton = new JButton("Long List");
        sendLongListButton.addActionListener(e -> sendLongListMessage());
        
        buttonPanel.add(sendHelloWorldButton);
        buttonPanel.add(sendLongListButton);
        
        mainPanel.add(buttonPanel, BorderLayout.EAST);
        add(mainPanel);
    }

    private void setupControlPanel() {
        var controlPanel = new JPanel(new FlowLayout());
        
        controlPanel.add(new JLabel("Port:"));
        controlPanel.add(availablePortsDropdown);
        
        connectionToggleButton.addActionListener(e -> toggleSerialConnection());
        controlPanel.add(connectionToggleButton);
        
        var clearOutputButton = new JButton("Clear");
        clearOutputButton.addActionListener(e -> serialOutputArea.setText(""));
        controlPanel.add(clearOutputButton);
        
        add(controlPanel, BorderLayout.NORTH);
    }

    private void updateAvailablePorts() {
        SwingUtilities.invokeLater(this::refreshPortList);
    }

    void refreshPortList() {
        var currentSelection = (String) availablePortsDropdown.getSelectedItem();
        availablePortsDropdown.removeAllItems();

        String[] detectedPorts = portNamesProvider.get();
        for (String port : detectedPorts) {
            availablePortsDropdown.addItem(port);
        }

        if (currentSelection != null) {
            availablePortsDropdown.setSelectedItem(currentSelection);
        }
    }

    private void toggleSerialConnection() {
        if (isSerialPortConnected) {
            disconnectSerialPort();
        } else {
            connectToSerialPort();
        }
    }

    private void connectToSerialPort() {
        String selectedPort = (String) availablePortsDropdown.getSelectedItem();
        if (selectedPort == null) {
            showError("No port selected");
            return;
        }

        try {
            activeSerialPort = new SerialPort(selectedPort);
            if (activeSerialPort.openPort()) {
                activeSerialPort.setParams(
                    selectedBaudRate,
                    selectedDataBits,
                    selectedStopBits,
                    selectedParity
                );
                
                activeSerialPort.addEventListener((SerialPortEvent event) -> {
                    if (event.isRXCHAR() && event.getEventValue() > 0) {
                        try {
                            String receivedData = activeSerialPort.readString(event.getEventValue());
                            SwingUtilities.invokeLater(() -> 
                                serialOutputArea.append("Received: " + receivedData + "\n")
                            );
                        } catch (SerialPortException ex) {
                            showError("Error reading from port: " + ex.getMessage());
                        }
                    }
                }, SerialPort.MASK_RXCHAR);
                
                isSerialPortConnected = true;
                connectionToggleButton.setText("Disconnect");
                serialOutputArea.append("Connected to " + selectedPort + "\n");
            } else {
                showError("Failed to open port");
            }
        } catch (SerialPortException ex) {
            showError("Error opening port: " + ex.getMessage());
        }
    }

    private void disconnectSerialPort() {
        if (activeSerialPort != null) {
            try {
                activeSerialPort.closePort();
            } catch (SerialPortException ex) {
                // Ignore close errors
            } finally {
                activeSerialPort = null;
            }
        }
        isSerialPortConnected = false;
        connectionToggleButton.setText("Connect");
        serialOutputArea.append("Disconnected\n");
    }

    private void sendSerialMessage(String message) {
        if (!isSerialPortConnected || activeSerialPort == null) {
            showError("Not connected to any port");
            return;
        }

        try {
            activeSerialPort.writeString(message);
            serialOutputArea.append("Sent: " + message + "\n");
        } catch (SerialPortException ex) {
            showError("Error sending data: " + ex.getMessage());
        }
    }

    private void saveOutputToFile() {
        var fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(Path.of(fileChooser.getSelectedFile().getPath()), serialOutputArea.getText());
            } catch (IOException ex) {
                showError("Error saving file: " + ex.getMessage());
            }
        }
    }

    private void showSettingsDialog() {
        var settingsPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        var baudRateOptions = List.of("9600", "14400", "19200", "28800", "38400", "57600", "115200");
        var baudRateDropdown = new JComboBox<>(baudRateOptions.toArray(new String[0]));
        baudRateDropdown.setSelectedItem(String.valueOf(selectedBaudRate));
        
        var parityOptions = List.of("None", "Odd", "Even", "Mark", "Space");
        var parityDropdown = new JComboBox<>(parityOptions.toArray(new String[0]));
        
        var dataBitsField = new JTextField(String.valueOf(selectedDataBits));
        var stopBitsField = new JTextField(String.valueOf(selectedStopBits));
        
        settingsPanel.add(new JLabel("Baud Rate:"));
        settingsPanel.add(baudRateDropdown);
        settingsPanel.add(new JLabel("Data Bits:"));
        settingsPanel.add(dataBitsField);
        settingsPanel.add(new JLabel("Stop Bits:"));
        settingsPanel.add(stopBitsField);
        settingsPanel.add(new JLabel("Parity:"));
        settingsPanel.add(parityDropdown);
        
        if (JOptionPane.showConfirmDialog(this, settingsPanel, "Settings",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                selectedBaudRate = Integer.parseInt(baudRateDropdown.getSelectedItem().toString());
                selectedDataBits = Integer.parseInt(dataBitsField.getText().trim());
                selectedStopBits = Integer.parseInt(stopBitsField.getText().trim());
                
                switch (parityDropdown.getSelectedIndex()) {
                    case 0 -> selectedParity = SerialPort.PARITY_NONE;
                    case 1 -> selectedParity = SerialPort.PARITY_ODD;
                    case 2 -> selectedParity = SerialPort.PARITY_EVEN;
                    case 3 -> selectedParity = SerialPort.PARITY_MARK;
                    case 4 -> selectedParity = SerialPort.PARITY_SPACE;
                }
                
                if (isSerialPortConnected) {
                    disconnectSerialPort();
                    connectToSerialPort();
                }
            } catch (NumberFormatException ex) {
                showError("Invalid number format in settings");
            }
        }
    }

    private void sendLongListMessage() {
        // Implementation for sending long list of words
        // This could be moved to a separate resource file in a production environment
        var words = "activate\nabort\nabridge\n..."; // shortened for brevity
        sendSerialMessage(words);
    }

    private void showError(String errorMessage) {
        errorHandler.accept(errorMessage);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new Gui().setVisible(true);
        });
    }
}
