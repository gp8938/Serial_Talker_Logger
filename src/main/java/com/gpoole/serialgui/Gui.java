package com.gpoole.serialgui;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
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
import java.util.function.Function;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Gui extends JFrame {
    private final JTextArea outputArea;
    private final JComboBox<String> portsDropdown;
    private final JButton connectButton;
    private final JTextField messageInput;
    private SerialPort activeSerialPort;
    private boolean connected = false;
    private boolean autoNegotiateSpeed = false;
    private int baudRate = 9600;
    private int dataBits = SerialPort.DATABITS_8;
    private int stopBits = SerialPort.STOPBITS_1;
    private int parity = SerialPort.PARITY_NONE;
    private final ScheduledExecutorService portUpdater;
    private final Supplier<String[]> portProvider;
    private final Consumer<String> errorHandler;
    private final Function<String, SerialPort> serialPortFactory;
    private SerialPortEventListener portListener;

    public Gui() {
        this(true, SerialPortList::getPortNames, null, SerialPort::new);
    }

    Gui(boolean startPortUpdater) {
        this(startPortUpdater, SerialPortList::getPortNames, null, SerialPort::new);
    }

    Gui(boolean startPortUpdater, Supplier<String[]> portNamesProvider, Consumer<String> errorHandler, Function<String, SerialPort> serialPortFactory) {
        this.portProvider = portNamesProvider;
        this.errorHandler = errorHandler != null
            ? errorHandler
            : msg -> JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        this.serialPortFactory = serialPortFactory != null ? serialPortFactory : SerialPort::new;
        portUpdater = Executors.newScheduledThreadPool(1, runnable -> {
            Thread t = new Thread(runnable, "port-list-updater");
            t.setDaemon(true);
            return t;
        });
        setupFrame();
        
        // Create main components
        outputArea = new JTextArea(15, 40);
        portsDropdown = new JComboBox<>();
        connectButton = new JButton("Connect");
        messageInput = new JTextField(30);
        
        setupMenuBar();
        setupMainPanel();
        setupControlPanel();
        
        // Start port list updater
        if (startPortUpdater) {
            portUpdater.scheduleAtFixedRate(this::updateAvailablePorts, 0, 2, TimeUnit.SECONDS);
        }
        
        // Add window closing handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectSerialPort();
                portUpdater.shutdownNow();
                dispose();
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
        settingsMenu.addSeparator();
        var autoNegotiateItem = new JCheckBoxMenuItem("Auto-Negotiate Speed");
        autoNegotiateItem.addActionListener(e -> autoNegotiateSpeed = autoNegotiateItem.isSelected());
        settingsMenu.add(autoNegotiateItem);
        
        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);
    }

    private void setupMainPanel() {
        var mainPanel = new JPanel(new BorderLayout());
        
        // Output area with scroll
        outputArea.setEditable(false);
        var scrollPane = new JScrollPane(outputArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Input panel on the right
        var inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.add(new JLabel("Message:"), BorderLayout.NORTH);
        inputPanel.add(messageInput, BorderLayout.CENTER);
        var sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendSerialMessage(messageInput.getText()));
        inputPanel.add(sendButton, BorderLayout.SOUTH);
        
        mainPanel.add(inputPanel, BorderLayout.EAST);
        add(mainPanel);
    }

    private void setupControlPanel() {
        var controlPanel = new JPanel(new FlowLayout());
        
        controlPanel.add(new JLabel("Port:"));
        controlPanel.add(portsDropdown);
        
        connectButton.addActionListener(e -> toggleSerialConnection());
        controlPanel.add(connectButton);
        
        var clearOutputButton = new JButton("Clear");
        clearOutputButton.addActionListener(e -> outputArea.setText(""));
        controlPanel.add(clearOutputButton);
        
        add(controlPanel, BorderLayout.NORTH);
    }

    private void updateAvailablePorts() {
        SwingUtilities.invokeLater(this::refreshPortList);
    }

    void refreshPortList() {
        var currentSelection = (String) portsDropdown.getSelectedItem();
        portsDropdown.removeAllItems();

        String[] detectedPorts = portProvider.get();
        
        if (detectedPorts == null || detectedPorts.length == 0) {
            portsDropdown.addItem("No COM ports found");
            portsDropdown.setEnabled(false);
            connectButton.setEnabled(false);
            showError("Warning: No COM ports found");
        } else {
            portsDropdown.setEnabled(true);
            connectButton.setEnabled(true);
            for (String port : detectedPorts) {
                portsDropdown.addItem(port);
            }
            if (currentSelection != null && portsDropdown.getModel().getSize() > 0) {
                portsDropdown.setSelectedItem(currentSelection);
            }
        }
    }

    private void toggleSerialConnection() {
        if (connected) {
            disconnectSerialPort();
        } else {
            connectToSerialPort();
        }
    }

    private void connectToSerialPort() {
        String selectedPort = (String) portsDropdown.getSelectedItem();
        if (selectedPort == null) {
            showError("No port selected");
            return;
        }

        try {
            activeSerialPort = serialPortFactory.apply(selectedPort);
            if (activeSerialPort.openPort()) {
                activeSerialPort.setParams(
                    baudRate,
                    dataBits,
                    stopBits,
                    parity
                );
                
                portListener = (SerialPortEvent event) -> {
                    if (event.isRXCHAR() && event.getEventValue() > 0) {
                        try {
                            String receivedData = activeSerialPort.readString(event.getEventValue());
                            SwingUtilities.invokeLater(() ->
                                outputArea.append("Received: " + receivedData + "\n")
                            );
                        } catch (SerialPortException ex) {
                            showError("Error reading from port: " + ex.getMessage());
                        }
                    }
                };

                activeSerialPort.addEventListener(portListener, SerialPort.MASK_RXCHAR);
                
                connected = true;
                connectButton.setText("Disconnect");
                outputArea.append("Connected to " + selectedPort + "\n");
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
                if (portListener != null) {
                    activeSerialPort.removeEventListener();
                }
                activeSerialPort.closePort();
            } catch (SerialPortException ex) {
                // Ignore close errors
            } finally {
                activeSerialPort = null;
                portListener = null;
            }
        }
        connected = false;
        connectButton.setText("Connect");
        outputArea.append("Disconnected\n");
    }

    private void sendSerialMessage(String message) {
        if (!connected || activeSerialPort == null) {
            showError("Not connected to any port");
            return;
        }

        try {
            activeSerialPort.writeString(message);
            outputArea.append("Sent: " + message + "\n");
        } catch (SerialPortException ex) {
            showError("Error sending data: " + ex.getMessage());
        }
    }

    private void saveOutputToFile() {
        var fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(Path.of(fileChooser.getSelectedFile().getPath()), outputArea.getText());
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
        baudRateDropdown.setSelectedItem(String.valueOf(baudRate));
        
        var parityOptions = List.of("None", "Odd", "Even", "Mark", "Space");
        var parityDropdown = new JComboBox<>(parityOptions.toArray(new String[0]));
        
        var dataBitsField = new JTextField(String.valueOf(dataBits));
        var stopBitsField = new JTextField(String.valueOf(stopBits));
        
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
                baudRate = Integer.parseInt(baudRateDropdown.getSelectedItem().toString());
                dataBits = Integer.parseInt(dataBitsField.getText().trim());
                stopBits = Integer.parseInt(stopBitsField.getText().trim());
                
                switch (parityDropdown.getSelectedIndex()) {
                    case 0 -> parity = SerialPort.PARITY_NONE;
                    case 1 -> parity = SerialPort.PARITY_ODD;
                    case 2 -> parity = SerialPort.PARITY_EVEN;
                    case 3 -> parity = SerialPort.PARITY_MARK;
                    case 4 -> parity = SerialPort.PARITY_SPACE;
                }
                
                if (connected) {
                    disconnectSerialPort();
                    connectToSerialPort();
                }
            } catch (NumberFormatException ex) {
                showError("Invalid number format in settings");
            }
        }
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
