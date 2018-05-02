/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serialgui;

import com.fazecast.jSerialComm.SerialPort;
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @author Geoff
 */
public class Serial {

    SerialPort comPort;

    public Serial() {

    }

    public void writeto(String command) {
        byte[] buffer = command.getBytes();
        comPort.writeBytes(buffer, buffer.length);
    }

    public void setPortParameters(int baudfromGUI, int dataBits, int endBits, int Parity) {
        /*
        "static final public int NO_PARITY = 0;
 	static final public int ODD_PARITY = 1;
 	static final public int EVEN_PARITY = 2;
 	static final public int MARK_PARITY = 3;
 	static final public int SPACE_PARITY = 4;"
        from SerialPort.java
         */
        comPort.setComPortParameters(baudfromGUI, dataBits, endBits, Parity);
    }

    public void connectTo(String port) {
        comPort = com.fazecast.jSerialComm.SerialPort.getCommPort(port);
        comPort.setComPortTimeouts(com.fazecast.jSerialComm.SerialPort.TIMEOUT_SCANNER, 0, 0);
    }

    public boolean openPort() {
        return comPort.openPort();
    }

    public String getData() {
        String line = "";
        try (Scanner scanner = new Scanner(comPort.getInputStream())) {
            while (scanner.hasNextLine()) {
                try {
                    line = scanner.nextLine();
                } catch (Exception e) {
                    System.out.print(Arrays.toString(e.getStackTrace()));
                }
            }
        }
        return line;
    }

    public void closePort() {
        comPort.closePort();
    }
}
