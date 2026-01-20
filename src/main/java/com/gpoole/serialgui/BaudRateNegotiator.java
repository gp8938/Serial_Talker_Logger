package com.gpoole.serialgui;

import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * Handles automatic baud rate negotiation for serial ports.
 * Attempts to communicate at different baud rates to find the correct one.
 */
public class BaudRateNegotiator {
    private static final int[] COMMON_BAUD_RATES = {
        9600, 115200, 19200, 38400, 57600,
        14400, 28800, 4800, 2400, 1200
    };

    private static final String NEGOTIATE_COMMAND = "AT\r\n";
    private static final long TIMEOUT_MS = 500;

    /**
     * Attempts to negotiate the correct baud rate for a serial port.
     * Tries each common baud rate and sends a test command.
     *
     * @param port The serial port to negotiate
     * @param dataBits Number of data bits
     * @param stopBits Number of stop bits
     * @param parity Parity setting
     * @return The successfully negotiated baud rate, or -1 if none worked
     */
    public static int negotiate(SerialPort port, int dataBits, int stopBits, int parity) {
        for (int baudRate : COMMON_BAUD_RATES) {
            if (tryBaudRate(port, baudRate, dataBits, stopBits, parity)) {
                return baudRate;
            }
        }
        return -1;
    }

    /**
     * Attempts to communicate with a device at a specific baud rate.
     *
     * @param port The serial port
     * @param baudRate The baud rate to try
     * @param dataBits Number of data bits
     * @param stopBits Number of stop bits
     * @param parity Parity setting
     * @return True if communication was successful at this baud rate
     */
    private static boolean tryBaudRate(SerialPort port, int baudRate, int dataBits, int stopBits, int parity) {
        try {
            port.setParams(baudRate, dataBits, stopBits, parity);
            
            // Send a simple AT command and wait for response
            port.writeString(NEGOTIATE_COMMAND);
            
            // Wait a bit for response
            Thread.sleep(TIMEOUT_MS);
            
            // Check if we have data to read
            if (port.getInputBufferBytesCount() > 0) {
                String response = port.readString();
                // Check for typical AT command responses (OK, ERROR, +, etc.)
                if (response != null && !response.isEmpty() &&
                    (response.contains("OK") || response.contains("ERROR") || 
                     response.contains("+") || response.length() > 0)) {
                    return true;
                }
            }
        } catch (SerialPortException | InterruptedException e) {
            // This baud rate didn't work, try the next one
        }
        return false;
    }
}
