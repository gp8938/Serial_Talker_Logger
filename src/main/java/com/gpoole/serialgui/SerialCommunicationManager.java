package com.gpoole.serialgui;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages serial port communication independently from the GUI.
 * Handles connection, disconnection, data sending/receiving, and event notifications.
 */
public class SerialCommunicationManager {
    private final Function<String, SerialPort> serialPortFactory;
    private final int defaultDataBits;
    private final int defaultStopBits;
    private final int defaultParity;
    
    private SerialPort activePort;
    private SerialPortEventListener portListener;
    private boolean connected = false;
    private long bytesSent = 0;
    private long bytesReceived = 0;
    private long connectionStartTime = 0;

    private Consumer<String> onDataReceived;
    private Consumer<String> onError;
    private Consumer<String> onConnected;
    private Consumer<String> onDisconnected;

    /**
     * Creates a new SerialCommunicationManager.
     */
    public SerialCommunicationManager(Function<String, SerialPort> serialPortFactory,
                                     int defaultDataBits,
                                     int defaultStopBits,
                                     int defaultParity) {
        this.serialPortFactory = serialPortFactory;
        this.defaultDataBits = defaultDataBits;
        this.defaultStopBits = defaultStopBits;
        this.defaultParity = defaultParity;
        this.onDataReceived = str -> {};
        this.onError = str -> {};
        this.onConnected = str -> {};
        this.onDisconnected = str -> {};
    }

    /**
     * Sets the callback for when data is received.
     */
    public SerialCommunicationManager onDataReceived(Consumer<String> callback) {
        this.onDataReceived = callback;
        return this;
    }

    /**
     * Sets the callback for when an error occurs.
     */
    public SerialCommunicationManager onError(Consumer<String> callback) {
        this.onError = callback;
        return this;
    }

    /**
     * Sets the callback for when successfully connected.
     */
    public SerialCommunicationManager onConnected(Consumer<String> callback) {
        this.onConnected = callback;
        return this;
    }

    /**
     * Sets the callback for when disconnected.
     */
    public SerialCommunicationManager onDisconnected(Consumer<String> callback) {
        this.onDisconnected = callback;
        return this;
    }

    /**
     * Connects to a serial port with specified parameters.
     *
     * @param portName The name of the port (e.g., "COM1")
     * @param baudRate The baud rate
     * @param dataBits Number of data bits
     * @param stopBits Number of stop bits
     * @param parity Parity setting
     * @return True if connection was successful
     */
    public boolean connect(String portName, int baudRate, int dataBits, int stopBits, int parity) {
        try {
            activePort = serialPortFactory.apply(portName);
            if (activePort.openPort()) {
                activePort.setParams(baudRate, dataBits, stopBits, parity);

                setupEventListener();
                activePort.addEventListener(portListener, SerialPort.MASK_RXCHAR);

                bytesSent = 0;
                bytesReceived = 0;
                connectionStartTime = System.currentTimeMillis();
                connected = true;

                onConnected.accept(portName);
                return true;
            } else {
                onError.accept("Failed to open port: " + portName);
                return false;
            }
        } catch (SerialPortException ex) {
            onError.accept("Error opening port: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Sets up the event listener for receiving data.
     */
    private void setupEventListener() {
        portListener = (SerialPortEvent event) -> {
            if (event.isRXCHAR() && event.getEventValue() > 0) {
                try {
                    String receivedData = activePort.readString(event.getEventValue());
                    bytesReceived += receivedData.length();
                    onDataReceived.accept(receivedData);
                } catch (SerialPortException ex) {
                    onError.accept("Error reading from port: " + ex.getMessage());
                }
            }
        };
    }

    /**
     * Disconnects from the serial port.
     */
    public void disconnect() {
        if (activePort != null) {
            try {
                if (portListener != null) {
                    activePort.removeEventListener();
                }
                activePort.closePort();
            } catch (SerialPortException ex) {
                // Ignore close errors
            } finally {
                activePort = null;
                portListener = null;
                connected = false;
            }
        }
        onDisconnected.accept("Disconnected");
    }

    /**
     * Sends a message through the serial port.
     *
     * @param message The message to send
     * @throws SerialPortException If not connected or write fails
     */
    public void sendMessage(String message) throws SerialPortException {
        if (!connected || activePort == null) {
            throw new SerialPortException("", "", "Not connected to any port");
        }
        activePort.writeString(message);
        bytesSent += message.length();
    }

    /**
     * Gets the number of bytes sent in current session.
     *
     * @return Bytes sent
     */
    public long getBytesSent() {
        return bytesSent;
    }

    /**
     * Gets the number of bytes received in current session.
     *
     * @return Bytes received
     */
    public long getBytesReceived() {
        return bytesReceived;
    }

    /**
     * Gets the connection uptime in seconds.
     *
     * @return Uptime in seconds, or 0 if not connected
     */
    public long getUptimeSeconds() {
        if (!connected || connectionStartTime == 0) {
            return 0;
        }
        return (System.currentTimeMillis() - connectionStartTime) / 1000;
    }

    /**
     * Checks if currently connected to a port.
     *
     * @return True if connected
     */
    public boolean isConnected() {
        return connected;
    }
}
