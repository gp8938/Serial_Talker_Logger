package com.gpoole.serialgui;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Main GUI window for serial communication application.
 * 
 * Provides a complete serial communication interface with:
 * - Port selection and connection management
 * - Real-time data display with timestamps and formatting options
 * - Text input with command history navigation
 * - Search/filter functionality
 * - CSV/JSON export capabilities
 * - Configuration persistence
 * - Performance metrics and connection status indicator
 * 
 * All serial communication is managed through the SerialCommunicationManager
 * to separate business logic from GUI concerns.
 */
public class Gui extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(Gui.class);
    
    private final JTextArea outputArea;
    private final JComboBox<String> portsDropdown;
    private final JButton connectButton;
    private final JTextField messageInput;
    private JLabel connectionStatusLabel;
    private boolean autoNegotiateSpeed = false;
    private boolean noPortsWarningShown = false;
    private int baudRate = 9600;
    private int dataBits = SerialPort.DATABITS_8;
    private int stopBits = SerialPort.STOPBITS_1;
    private int parity = SerialPort.PARITY_NONE;
    private final ScheduledExecutorService portUpdater;
    private final Supplier<String[]> portProvider;
    private final Consumer<String> errorHandler;
    private final Function<String, SerialPort> serialPortFactory;
    private final MessageFormatter messageFormatter;
    private final ConfigurationManager config;
    private final SerialCommunicationManager commManager;
    private final StatusLED statusLED;
    private boolean scrollLocked = false;
    private JLabel statusLabel;
    private final CommandHistory commandHistory;

    public Gui() {
        this(true, SerialPortList::getPortNames, null, SerialPort::new);
    }

    Gui(boolean startPortUpdater) {
        this(startPortUpdater, SerialPortList::getPortNames, null, SerialPort::new);
    }

    Gui(boolean startPortUpdater, Supplier<String[]> portNamesProvider, Consumer<String> errorHandler, Function<String, SerialPort> serialPortFactory) {
        this.config = new ConfigurationManager();
        this.messageFormatter = new MessageFormatter(MessageFormatter.DisplayMode.ASCII);
        this.portProvider = portNamesProvider;
        this.errorHandler = errorHandler != null
            ? errorHandler
            : msg -> JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        this.serialPortFactory = serialPortFactory != null ? serialPortFactory : SerialPort::new;
        
        // Initialize SerialCommunicationManager with callbacks
        this.commManager = new SerialCommunicationManager(serialPortFactory, 
            dataBits, stopBits, parity);
        this.commManager.onDataReceived(this::onDataReceived);
        this.commManager.onError(this::onError);
        this.commManager.onConnected(this::onConnected);
        this.commManager.onDisconnected(this::onDisconnected);
        
        this.statusLED = new StatusLED();
        this.commandHistory = new CommandHistory();
        
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
                saveConfiguration();
                commManager.disconnect();
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
        restoreConfiguration();
    }

    private void setupMenuBar() {
        var menuBar = new JMenuBar();
        
        // File Menu
        var fileMenu = new JMenu("File");
        var saveMenuItem = new JMenuItem("Save as Text");
        saveMenuItem.addActionListener(e -> saveOutputToFile());
        var csvMenuItem = new JMenuItem("Export as CSV");
        csvMenuItem.addActionListener(e -> exportAsCSV());
        var jsonMenuItem = new JMenuItem("Export as JSON");
        jsonMenuItem.addActionListener(e -> exportAsJSON());
        var exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> System.exit(0));
        fileMenu.add(saveMenuItem);
        fileMenu.add(csvMenuItem);
        fileMenu.add(jsonMenuItem);
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
        
        // Output area with scroll - takes most of the space
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        var scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Communication Log"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Input panel at the bottom for natural workflow
        var inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.setBackground(outputArea.getBackground());
        
        var inputLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        inputLabelPanel.setBackground(outputArea.getBackground());
        inputLabelPanel.add(new JLabel("Message:"));
        inputLabelPanel.setPreferredSize(new Dimension(100, 25));
        
        var commandPanel = new JPanel(new BorderLayout(5, 0));
        commandPanel.setBackground(outputArea.getBackground());
        commandPanel.add(messageInput, BorderLayout.CENTER);
        
        var sendButton = new JButton("Send (Ctrl+Enter)");
        sendButton.setToolTipText("Send message or press Ctrl+Enter");
        sendButton.addActionListener(e -> {
            String message = messageInput.getText();
            sendSerialMessage(message);
            if (!message.trim().isEmpty()) {
                commandHistory.add(message);
            }
            messageInput.setText("");
        });
        commandPanel.add(sendButton, BorderLayout.EAST);
        
        inputPanel.add(inputLabelPanel, BorderLayout.WEST);
        inputPanel.add(commandPanel, BorderLayout.CENTER);
        
        // Add keyboard shortcuts
        messageInput.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    String message = messageInput.getText();
                    sendSerialMessage(message);
                    if (!message.trim().isEmpty()) {
                        commandHistory.add(message);
                    }
                    messageInput.setText("");
                    commandHistory.reset();
                    e.consume();
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                    String previous = commandHistory.getPrevious();
                    messageInput.setText(previous);
                    messageInput.setCaretPosition(previous.length());
                    e.consume();
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                    String next = commandHistory.getNext();
                    messageInput.setText(next);
                    messageInput.setCaretPosition(next.length());
                    e.consume();
                }
            }
        });
        
        outputArea.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_L) {
                    outputArea.setText("");
                    e.consume();
                } else if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_S) {
                    saveOutputToFile();
                    e.consume();
                } else if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_F) {
                    openSearchDialog();
                    e.consume();
                }
            }
        });
        
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    private void setupControlPanel() {
        var controlPanel = new JPanel(new FlowLayout());
        
        // Add status LED indicator
        controlPanel.add(statusLED);
        
        // Connection status label next to LED
        connectionStatusLabel = new JLabel("Disconnected");
        connectionStatusLabel.setToolTipText("Current connection status and details");
        controlPanel.add(connectionStatusLabel);
        
        controlPanel.add(new JLabel("Port:"));
        controlPanel.add(portsDropdown);
        
        connectButton.addActionListener(e -> toggleSerialConnection());
        controlPanel.add(connectButton);
        
        var clearOutputButton = new JButton("Clear");
        clearOutputButton.setToolTipText("Clear output area (Ctrl+L)");
        clearOutputButton.addActionListener(e -> outputArea.setText(""));
        controlPanel.add(clearOutputButton);
        
        var scrollLockCheckbox = new JCheckBox("Scroll Lock");
        scrollLockCheckbox.setToolTipText("Pause output without disconnecting. Messages will be buffered.");
        scrollLockCheckbox.addActionListener(e -> {
            scrollLocked = scrollLockCheckbox.isSelected();
            // Visual feedback when scroll lock active
            if (scrollLocked) {
                outputArea.setBackground(new Color(255, 255, 200)); // Light yellow
                outputArea.setToolTipText("Scroll Lock active - new messages are buffered");
            } else {
                outputArea.setBackground(Color.WHITE);
                outputArea.setToolTipText("Communication log");
            }
        });
        controlPanel.add(scrollLockCheckbox);
        
        add(controlPanel, BorderLayout.NORTH);
        
        // Add status bar for metrics
        var statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);
        
        // Update status and connection label periodically
        var statusUpdater = Executors.newScheduledThreadPool(1, runnable -> {
            Thread t = new Thread(runnable, "status-updater");
            t.setDaemon(true);
            return t;
        });
        statusUpdater.scheduleAtFixedRate(() -> {
            final String status;
            status = String.format("Bytes Sent: %d | Bytes Received: %d", 
                commManager.getBytesSent(), commManager.getBytesReceived());
            if (commManager.isConnected()) {
                long elapsedSec = commManager.getUptimeSeconds();
                long hours = elapsedSec / 3600;
                long minutes = (elapsedSec % 3600) / 60;
                long seconds = elapsedSec % 60;
                final String finalStatus = status + String.format(" | Uptime: %02d:%02d:%02d", hours, minutes, seconds);
                SwingUtilities.invokeLater(() -> statusLabel.setText(finalStatus));
            } else {
                SwingUtilities.invokeLater(() -> statusLabel.setText(status));
            }
        }, 1, 1, TimeUnit.SECONDS);
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
            if (!noPortsWarningShown) {
                showError("Warning: No COM ports found");
                noPortsWarningShown = true;
            }
        } else {
            noPortsWarningShown = false;
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
        if (commManager.isConnected()) {
            commManager.disconnect();
        } else {
            connectToSerialPort();
        }
    }

    private void connectToSerialPort() {
        String selectedPort = (String) portsDropdown.getSelectedItem();
        if (selectedPort == null || "No COM ports found".equals(selectedPort)) {
            logger.warn("Connection attempt with no port selected");
            showError("No port selected");
            return;
        }

        try {
            logger.info("Attempting to connect to port: {}", selectedPort);
            int actualBaudRate = baudRate;
            if (autoNegotiateSpeed) {
                logger.debug("Auto-negotiating baud rate");
                actualBaudRate = BaudRateNegotiator.negotiate(
                    serialPortFactory.apply(selectedPort), dataBits, stopBits, parity
                );
                if (actualBaudRate > 0) {
                    logger.info("Auto-negotiated baud rate: {}", actualBaudRate);
                    outputArea.append(messageFormatter.format("Auto-negotiated baud rate: " + actualBaudRate, false) + "\n");
                } else {
                    logger.warn("Failed to negotiate baud rate. Using default: {}", baudRate);
                    showError("Failed to negotiate baud rate. Using default: " + baudRate);
                    actualBaudRate = baudRate;
                }
            }
            
            commManager.connect(selectedPort, actualBaudRate, dataBits, stopBits, parity);
        } catch (Exception ex) {
            logger.error("Error opening port: {}", ex.getMessage(), ex);
            showError("Error opening port: " + ex.getMessage());
        }
    }

    private void onDataReceived(String data) {
        if (!scrollLocked) {
            SwingUtilities.invokeLater(() ->
                outputArea.append(messageFormatter.format(data, true) + "\n")
            );
        }
    }

    private void onConnected(String portName) {
        SwingUtilities.invokeLater(() -> {
            connectButton.setText("Disconnect");
            statusLED.setConnected(true);
            String statusText = String.format("Connected to %s @ %d baud", portName, baudRate);
            connectionStatusLabel.setText(statusText);
            connectionStatusLabel.setForeground(new Color(0, 128, 0)); // Green
            outputArea.append(messageFormatter.format("Connected to " + portName, false) + "\n");
            logger.info("Connection status updated: {}", statusText);
        });
    }

    private void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            connectButton.setText("Connect");
            statusLED.setConnected(false);
            connectionStatusLabel.setText("Disconnected");
            connectionStatusLabel.setForeground(new Color(192, 0, 0)); // Dark red
            outputArea.append(messageFormatter.format("Disconnected: " + reason, false) + "\n");
        });
    }

    private void onError(String errorMessage) {
        SwingUtilities.invokeLater(() -> showError(errorMessage));
    }

    private void sendSerialMessage(String message) {
        if (!commManager.isConnected()) {
            logger.warn("Send attempt while not connected");
            showError("Not connected to any port");
            return;
        }

        try {
            logger.debug("Sending message: {}", message);
            commManager.sendMessage(message);
            outputArea.append(messageFormatter.format(message, false) + "\n");
        } catch (Exception ex) {
            logger.error("Error sending data: {}", ex.getMessage(), ex);
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

    private void openSearchDialog() {
        var searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        var searchField = new JTextField(20);
        var resultLabel = new JLabel("Enter search term");
        var nextButton = new JButton("Next (→)");
        var prevButton = new JButton("← Previous");
        var highlightCheckbox = new JCheckBox("Highlight All", false);
        
        var currentIndex = new int[]{0};
        var lastSearchTerm = new String[]{""};
        
        Runnable performSearch = () -> {
            String searchTerm = searchField.getText();
            if (searchTerm.isEmpty()) {
                resultLabel.setText("Enter search term");
                return;
            }
            
            String text = outputArea.getText();
            int index = text.indexOf(searchTerm, currentIndex[0]);
            
            if (index == -1 && currentIndex[0] > 0) {
                index = text.indexOf(searchTerm, 0);
                if (index == -1) {
                    resultLabel.setText("No matches found");
                    return;
                }
            }
            
            if (index != -1) {
                outputArea.setCaretPosition(index);
                outputArea.select(index, index + searchTerm.length());
                currentIndex[0] = index + searchTerm.length();
                resultLabel.setText("Match found at position " + index);
                
                if (highlightCheckbox.isSelected()) {
                    highlightAllMatches(searchTerm);
                }
            } else {
                resultLabel.setText("No matches found");
            }
        };
        
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    currentIndex[0] = 0;
                    lastSearchTerm[0] = searchField.getText();
                    performSearch.run();
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    JOptionPane.getRootFrame().dispose();
                }
            }
        });
        
        nextButton.addActionListener(e -> performSearch.run());
        prevButton.addActionListener(e -> {
            String searchTerm = searchField.getText();
            if (searchTerm.isEmpty()) {
                return;
            }
            String text = outputArea.getText();
            int index = text.lastIndexOf(searchTerm, currentIndex[0] - searchTerm.length() - 1);
            if (index != -1) {
                outputArea.setCaretPosition(index);
                outputArea.select(index, index + searchTerm.length());
                currentIndex[0] = index;
                resultLabel.setText("Match found at position " + index);
            }
        });
        
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(highlightCheckbox);
        
        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        var dialog = new JDialog(this, "Search", false);
        dialog.getContentPane().add(searchPanel);
        dialog.add(resultLabel, BorderLayout.SOUTH);
        dialog.setSize(400, 120);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        searchField.requestFocus();
        
        logger.debug("Search dialog opened");
    }

    private void highlightAllMatches(String searchTerm) {
        // Highlight all matches by finding them all and displaying count
        String text = outputArea.getText();
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(searchTerm, index)) != -1) {
            count++;
            index += searchTerm.length();
        }
        logger.debug("Found {} matches for '{}'", count, searchTerm);
    }

    private void exportAsCSV() {
        var fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("output.csv"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String text = outputArea.getText();
                String[] lines = text.split("\n");
                
                StringBuilder csv = new StringBuilder();
                csv.append("Timestamp,Source,Message\n");
                
                for (String line : lines) {
                    // Parse [HH:mm:ss.SSS] TX/RX: Message format
                    if (line.startsWith("[")) {
                        int closeIndex = line.indexOf("]");
                        String timestamp = line.substring(1, closeIndex);
                        String rest = line.substring(closeIndex + 2);
                        
                        int colonIndex = rest.indexOf(":");
                        String source = rest.substring(0, colonIndex).trim();
                        String message = rest.substring(colonIndex + 1).trim();
                        
                        csv.append("\"").append(timestamp).append("\",");
                        csv.append("\"").append(source).append("\",");
                        csv.append("\"").append(message).append("\"\n");
                    }
                }
                
                Files.writeString(Path.of(fileChooser.getSelectedFile().getPath()), csv.toString());
                logger.info("Exported data to CSV: {}", fileChooser.getSelectedFile().getPath());
            } catch (IOException ex) {
                logger.error("Error exporting CSV: {}", ex.getMessage(), ex);
                showError("Error exporting CSV: " + ex.getMessage());
            }
        }
    }

    private void exportAsJSON() {
        var fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("output.json"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String text = outputArea.getText();
                String[] lines = text.split("\n");
                
                StringBuilder json = new StringBuilder();
                json.append("{\n  \"messages\": [\n");
                
                boolean first = true;
                for (String line : lines) {
                    // Parse [HH:mm:ss.SSS] TX/RX: Message format
                    if (line.startsWith("[")) {
                        if (!first) json.append(",\n");
                        first = false;
                        
                        int closeIndex = line.indexOf("]");
                        String timestamp = line.substring(1, closeIndex);
                        String rest = line.substring(closeIndex + 2);
                        
                        int colonIndex = rest.indexOf(":");
                        String source = rest.substring(0, colonIndex).trim();
                        String message = rest.substring(colonIndex + 1).trim();
                        
                        json.append("    {\n");
                        json.append("      \"timestamp\": \"").append(escapeJson(timestamp)).append("\",\n");
                        json.append("      \"source\": \"").append(escapeJson(source)).append("\",\n");
                        json.append("      \"message\": \"").append(escapeJson(message)).append("\"\n");
                        json.append("    }");
                    }
                }
                
                json.append("\n  ]\n}\n");
                Files.writeString(Path.of(fileChooser.getSelectedFile().getPath()), json.toString());
                logger.info("Exported data to JSON: {}", fileChooser.getSelectedFile().getPath());
            } catch (IOException ex) {
                logger.error("Error exporting JSON: {}", ex.getMessage(), ex);
                showError("Error exporting JSON: " + ex.getMessage());
            }
        }
    }

    private String escapeJson(String str) {
        return str.replace("\"", "\\\"")
                  .replace("\\", "\\\\")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private void showSettingsDialog() {
        var settingsPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        var baudRateOptions = List.of("9600", "14400", "19200", "28800", "38400", "57600", "115200");
        var baudRateDropdown = new JComboBox<>(baudRateOptions.toArray(new String[0]));
        baudRateDropdown.setSelectedItem(String.valueOf(baudRate));
        
        var parityOptions = List.of("None", "Odd", "Even", "Mark", "Space");
        var parityDropdown = new JComboBox<>(parityOptions.toArray(new String[0]));
        
        var displayModeOptions = List.of("ASCII", "HEX", "HEX and ASCII");
        var displayModeDropdown = new JComboBox<>(displayModeOptions.toArray(new String[0]));
        displayModeDropdown.setSelectedIndex(0);
        
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
        settingsPanel.add(new JLabel("Display Mode:"));
        settingsPanel.add(displayModeDropdown);
        
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
                
                MessageFormatter.DisplayMode newMode = switch (displayModeDropdown.getSelectedIndex()) {
                    case 0 -> MessageFormatter.DisplayMode.ASCII;
                    case 1 -> MessageFormatter.DisplayMode.HEX;
                    case 2 -> MessageFormatter.DisplayMode.HEX_AND_ASCII;
                    default -> MessageFormatter.DisplayMode.ASCII;
                };
                
                config.setString(ConfigurationManager.KEY_DISPLAY_MODE, newMode.name());
                messageFormatter.setDisplayMode(newMode);
                
                if (commManager.isConnected()) {
                    commManager.disconnect();
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

    private void saveConfiguration() {
        config.setInt(ConfigurationManager.KEY_BAUD_RATE, baudRate);
        config.setInt(ConfigurationManager.KEY_DATA_BITS, dataBits);
        config.setInt(ConfigurationManager.KEY_STOP_BITS, stopBits);
        config.setInt(ConfigurationManager.KEY_PARITY, parity);
        config.setBoolean(ConfigurationManager.KEY_AUTO_NEGOTIATE, autoNegotiateSpeed);
        config.setInt(ConfigurationManager.KEY_WINDOW_WIDTH, getWidth());
        config.setInt(ConfigurationManager.KEY_WINDOW_HEIGHT, getHeight());
        config.setInt(ConfigurationManager.KEY_WINDOW_X, getX());
        config.setInt(ConfigurationManager.KEY_WINDOW_Y, getY());
        Object selectedPort = portsDropdown.getSelectedItem();
        if (selectedPort != null && !selectedPort.toString().equals("No COM ports found")) {
            config.setString(ConfigurationManager.KEY_LAST_PORT, selectedPort.toString());
        }
        config.saveConfiguration();
    }

    private void restoreConfiguration() {
        baudRate = config.getInt(ConfigurationManager.KEY_BAUD_RATE, 9600);
        dataBits = config.getInt(ConfigurationManager.KEY_DATA_BITS, SerialPort.DATABITS_8);
        stopBits = config.getInt(ConfigurationManager.KEY_STOP_BITS, SerialPort.STOPBITS_1);
        parity = config.getInt(ConfigurationManager.KEY_PARITY, SerialPort.PARITY_NONE);
        autoNegotiateSpeed = config.getBoolean(ConfigurationManager.KEY_AUTO_NEGOTIATE, false);
        
        int width = config.getInt(ConfigurationManager.KEY_WINDOW_WIDTH, 800);
        int height = config.getInt(ConfigurationManager.KEY_WINDOW_HEIGHT, 600);
        int x = config.getInt(ConfigurationManager.KEY_WINDOW_X, -1);
        int y = config.getInt(ConfigurationManager.KEY_WINDOW_Y, -1);
        
        setSize(width, height);
        if (x >= 0 && y >= 0) {
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null);
        }
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
