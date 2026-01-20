package com.gpoole.serialgui;

import javax.swing.*;
import java.awt.*;

/**
 * A visual LED indicator component showing connection status.
 * Displays as a colored circle - green when connected, red when disconnected.
 */
public class StatusLED extends JPanel {
    private boolean connected = false;
    private static final int SIZE = 20;
    private static final Color CONNECTED_COLOR = new Color(0, 255, 0);
    private static final Color DISCONNECTED_COLOR = new Color(255, 0, 0);

    /**
     * Creates a new StatusLED component.
     */
    public StatusLED() {
        setPreferredSize(new Dimension(SIZE + 10, SIZE + 10));
        setOpaque(false);
    }

    /**
     * Sets the connection status and updates the display.
     *
     * @param connected True if connected, false otherwise
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
        repaint();
    }

    /**
     * Gets the current connection status.
     *
     * @return True if connected
     */
    public boolean isConnected() {
        return connected;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the LED circle
        Color color = connected ? CONNECTED_COLOR : DISCONNECTED_COLOR;
        g2d.setColor(color);
        g2d.fillOval(5, 5, SIZE, SIZE);

        // Draw border
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(5, 5, SIZE, SIZE);

        // Draw highlight for 3D effect
        if (connected) {
            g2d.setColor(new Color(0, 255, 0, 100));
            g2d.fillOval(7, 7, 6, 6);
        }
    }
}
