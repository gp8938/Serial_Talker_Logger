import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;

public class Gui extends JFrame {
    private JTextArea outputArea;
    private JButton connectButton;
    private JComboBox<String> portList;
    private JTextField baudField, dataBitsField, stopBitsField, parityField;
    private SerialPort serialPort;
    private boolean isConnected = false;
    public Gui() {
        setTitle("Serial Communication GUI");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set up the menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.addActionListener(e -> {
            String text = outputArea.getText();
            try (java.io.FileWriter fw = new java.io.FileWriter("output.txt")) {
                fw.write(text);
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> System.exit(0));
        fileMenu.add(saveMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);

        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(e -> {
            String[] baudRates = {"9600", "14400", "19200", "28800", "38400", "57600", "115200"};
            JComboBox<String> baudCombo = new JComboBox<>(baudRates);
            String[] parityOptions = {"None", "Odd", "Even", "Mark", "Space"};
            JComboBox<String> parityCombo = new JComboBox<>(parityOptions);
            JTextField dataBits = new JTextField("8");
            JTextField stopBits = new JTextField("1");
            JPanel panel = new JPanel(new GridLayout(0, 2));
            panel.add(new JLabel("Baud Rate:"));
            panel.add(baudCombo);
        panel.add(new JLabel("Data Bits:"));
            panel.add(dataBits);
        panel.add(new JLabel("Stop Bits:"));
            panel.add(stopBits);
            panel.add(new JLabel("Parity:"));
            panel.add(parityCombo);
            int result = JOptionPane.showConfirmDialog(this, panel, "Settings", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
                baudRate = Integer.parseInt(baudCombo.getSelectedItem().toString());
                dataBitsValue = Integer.parseInt(dataBits.getText().trim());
                stopBitsValue = Integer.parseInt(stopBits.getText().trim());
                String selectedParity = parityCombo.getSelectedItem().toString();
                switch (selectedParity) {
                case "None":
                    parity = 0;
                    break;
                case "Odd":
                    parity = 1;
                    break;
                case "Even":
                    parity = 2;
                    break;
                case "Mark":
                    parity = 3;
                    break;
                case "Space":
                    parity = 4;
                    break;
                default:
                    parity = 0;
            }
        }
        });
        settingsMenu.add(settingsItem);
        menuBar.add(settingsMenu);

        setJMenuBar(menuBar);

        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // Output area
        outputArea = new JTextArea(15, 40);
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            String selectedPort = (String) portList.getSelectedItem();
        try {
                serialPort = new SerialPort(selectedPort, baudRate, parity, dataBitsValue, stopBitsValue);
                if (serialPort.openPort()) {
                    isConnected = true;
                    connectButton.setText("Disconnect");
                    outputArea.append("Connected to " + selectedPort + "\n");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to open port.", "Error", JOptionPane.ERROR_MESSAGE);
        }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid port selection.", "Error", JOptionPane.ERROR_MESSAGE);
    }
        });
        controlPanel.add(connectButton);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> {
            if (isConnected && serialPort != null) {
                String message = outputArea.getText();
        try {
                    serialPort.writeBytes(message.getBytes());
                    outputArea.append("Sent: " + message + "\n");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error sending data.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Not connected to any port.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        controlPanel.add(sendButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> outputArea.setText(""));
        controlPanel.add(clearButton);

        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Additional components
        JLabel portLabel = new JLabel("Select COM Port:");
        JComboBox<String> portsComboBox = new JComboBox<>();
        updatePortList();
        portList = portsComboBox;
        connectButton.addActionListener(e -> {
            String selectedPort = (String) portList.getSelectedItem();
            try {
                serialPort = new SerialPort(selectedPort, baudRate, parity, dataBitsValue, stopBitsValue);
                if (serialPort.openPort()) {
                    isConnected = true;
                    connectButton.setText("Disconnect");
                    outputArea.append("Connected to " + selectedPort + "\n");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to open port.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid port selection.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton helloWorldButton = new JButton("Hello World");
        helloWorldButton.addActionListener(e -> {
            String message = "Hello World!";
            try {
                serialPort.writeBytes(message.getBytes());
                outputArea.append("Sent: " + message + "\n");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error sending data.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton longListButton = new JButton("Long List");
        longListButton.addActionListener(e -> {
            String message =
                    "activate\n"
                            + "abort\n"
                            + "abridge\n"
                            + "absorb\n"
                            + "absurdity\n"
                            + "abstract\n"
                            + "abbreviate\n"
                            + "abduction\n"
                            + "abductee\n"
                            + "accelerate\n"
                            + "acid\n"
                            + "accentuate\n"
                            + "acceptance\n"
                            + "accident\n"
                            + "achieve\n"
                            + "acknowledge\n"
                            + "acquire\n"
                            + "act\n"
                            + "action\n"
                            + "activist\n"
                            + "active\n"
                            + "activity\n"
                            + "actor\n"
                            + "actualize\n"
                            + "adaptation\n"
                            + "addiction\n"
                            + "adjustment\n"
                            + "administer\n"
                            + "admit\n"
                            + "adopt\n"
                            + "advocate\n"
                            + "aerate\n"
                            + "affix\n"
                            + "aftermath\n"
                            + "agency\n"
                            + "agent\n"
                            + "aid\n"
                            + "aim\n"
                            + "air\n"
                            + "alarm\n"
                            + "allege\n"
                            + "alleyway\n"
                            + "alkali\n"
                            + "alligator\n"
                            + "allergy\n"
                            + "allocate\n"
                            + "allowance\n"
                            + "ambition\n"
                            + "amplify\n"
                            + "anatomical\n"
                            + "anatomy\n"
                            + "anchor\n"
                            + "android\n"
                            + "announce\n"
                            + "annihilate\n"
                            + "answer\n"
                            + "antacid\n"
                            + "antagonist\n"
                            + "anticipate\n"
                            + "anthropology\n"
                            + "antiquity\n"
                            + "apparatus\n"
                            + "apple\n"
                            + "appliance\n"
                            + "appropriate\n"
                            + "aquarium\n"
                            + "archive\n"
                            + "armchair\n"
                            + "arrangement\n"
                            + "array\n"
                            + "artist\n"
                            + "artistic\n"
                            + "aspiration\n"
                            + "assault\n"
                            + "assemble\n"
                            + "asset\n"
                            + "assign\n"
                            + "assist\n"
                            + "assembly\n"
                            + "asthma\n"
                            + "atmosphere\n"
                            + "atomistic\n"
                            + "atomic\n"
                            + "attachment\n"
                            + "attack\n"
                            + "aunt\n"
                            + "autumn\n"
                            + "avocado\n"
                            + "axiomatic\n"
                            + "backbone\n"
                            + "background\n"
                            + "backup\n"
                            + "balance\n"
                            + "ballistic\n"
                            + "balloon\n"
                            + "banjo\n"
                            + "bannister\n"
                            + "banana\n"
                            + "band\n"
                            + "bankrupt\n"
                            + "barber\n"
                            + "barefoot\n"
                            + "base\n"
                            + "basin\n"
                            + "basketball\n"
                            + "baton\n"
                            + "battery\n"
                            + "bay\n"
                            + "beach\n"
                            + "beagle\n"
                            + "beast\n"
                            + "beat\n"
                            + "beautiful\n"
                            + "because\n"
                            + "become\n"
                            + "bedrock\n"
                            + "bee\n"
                            + "befall\n"
                            + "beguile\n"
                            + "behave\n"
                            + "being\n"
                            + "bell\n"
                            + "beloved\n"
                            + "benefit\n"
                            + "berth\n"
                            + "besiege\n"
                            + "bestow\n"
                            + "betrayal\n"
                            + "betrothal\n"
                            + "bewilderment\n"
                            + "bias\n"
                            + "bibliography\n"
                            + "bicarbonate\n"
                            + "bicycle\n"
                            + "bigotry\n"
                            + "binomial\n"
                            + "bird\n"
                            + "birthplace\n"
                            + "bishop\n"
                            + "bituminous\n"
                            + "blame\n"
                            + "blindfold\n"
                            + "blockade\n"
                            + "blood\n"
                            + "blossom\n"
                            + "blot\n"
                            + "board\n"
                            + "boast\n"
                            + "boat\n"
                            + "bobcat\n"
                            + "bodyguard\n"
                            + "boldness\n"
                            + "bomb\n"
                            + "bone\n"
                            + "book\n"
                            + "boot\n"
                            + "bore\n"
                            + "bound\n"
                            + "boundary\n"
                            + "bounce\n"
                            + "bow\n"
                            + "boxer\n"
                            + "boxing\n"
                            + "boyfriend\n"
                            + "brainstorming\n"
                            + "branch\n"
                            + "brass\n"
                            + "brazil\n"
                            + "bread\n"
                            + "breakfast\n"
                            + "breath\n"
                            + "brew\n"
                            + "brigade\n"
                            + "broadcast\n"
                            + "bricklayer\n"
                            + "brighten\n"
                            + "bring\n"
                            + "broadcaster\n"
                            + "brow\n"
                            + "brunch\n"
                            + "brush\n"
                            + "bubble\n"
                            + "bucket\n"
                            + "budget\n"
                            + "buffalo\n"
                            + "building\n"
                            + "bulldozer\n"
                            + "bullfrog\n"
                            + "bunny\n"
                            + "burden\n"
                            + "burger\n"
                            + "burnish\n"
                            + "bury\n"
                            + "burst\n"
                            + "businessman\n"
                            + "busker\n"
                            + "butler\n"
                            + "butterfly\n"
                            + "button\n"
                            + "buyout\n"
                            + "cabaret\n"
                            + "cable\n"
                            + "caffeine\n"
                            + "café\n"
                            + "cake\n"
                            + "calamity\n"
                            + "calculator\n"
                            + "calendar\n"
                            + "calligraphy\n"
                            + "camera\n"
                            + "camper\n"
                            + "campaign\n"
                            + "canal\n"
                            + "cancer\n"
                            + "candle\n"
                            + "candy\n"
                            + "capacity\n"
                            + "captain\n"
                            + "carbohydrate\n"
                            + "cardinal\n"
                            + "carpet\n"
                            + "carry\n"
                            + "case\n"
                            + "cashier\n"
                            + "castle\n"
                            + "caterpillar\n"
                            + "category\n"
                            + "caution\n"
                            + "cavalry\n"
                            + "cease\n"
                            + "celebrity\n"
                            + "cell\n"
                            + "central\n"
                            + "centrifugal\n"
                            + "ceremony\n"
                            + "chain\n"
                            + "challenge\n"
                            + "chamber\n"
                            + "change\n"
                            + "characteristic\n"
                            + "chat\n"
                            + "cheap\n"
                            + "cheetah\n"
                            + "chemical\n"
                            + "chest\n"
                            + "childhood\n"
                            + "china\n"
                            + "chink\n"
                            + "chinook\n"
                            + "chitchat\n"
                            + "chip\n"
                            + "chirp\n"
                            + "cheerleader\n"
                            + "chiefdom\n"
                            + "chieftain\n"
                            + "childless\n"
                            + "children\n"
                            + "choice\n"
                            + "chocolate\n"
                            + "chokehold\n"
                            + "chromosome\n"
                            + "cicada\n"
                            + "circle\n"
                            + "citrus\n"
                            + "claim\n"
                            + "clam\n"
                            + "clarify\n"
                            + "classroom\n"
                            + "cleaning\n"
                            + "clear\n"
                            + "cleverness\n"
                            + "clipboard\n"
                            + "clock\n"
                            + "closet\n"
                            + "cloudy\n"
                            + "clue\n"
                            + "coach\n"
                            + "coast\n"
                            + "coat\n"
                            + "cobweb\n"
                            + "cockroach\n"
                            + "coffee\n"
                            + "coil\n"
                            + "coin\n"
                            + "colleague\n"
                            + "color\n"
                            + "column\n"
                            + "commence\n"
                            + "communication\n"
                            + "commute\n"
                            + "company\n"
                            + "competition\n"
                            + "compress\n"
                            + "concave\n"
                            + "confidence\n"
                            + "confectionery\n"
                            + "confirm\n"
                            + "confusion\n"
                            + "consent\n"
                            + "consumption\n"
                            + "contact\n"
                            + "contemplate\n"
                            + "continual\n"
                            + "contract\n"
                            + "contrast\n"
                            + "control\n"
                            + "consequence\n"
                            + "consciousness\n"
                            + "constellation\n"
                            + "constantly\n"
                            + "constructive\n"
                            + "constraint\n"
                            + "contact\n"
                            + "continue\n"
                            + "contract\n"
                            + "contrast\n"
                            + "control\n"
                            + "consequence\n"
                            + "consciousness\n"
                            + "constellation\n"
                            + "constantly\n"
                            + "constructive\n"
                            + "constraint\n"
                            + "contact\n"
                            + "continue\n"
                            + "contract\n"
                            + "contrast\n"
                            + "control\n"
                            + "consequence\n"
                            + "consciousness\n"
                            + "constellation\n"
                            + "constantly\n"
                            + "constructive\n"
                            + "constraint\n"
                            + "contact\n"
                            + "continue\n"
                            + "contract\n"
                            + "contrast\n"
                            + "control\n"
                            + "consequence\n"
                            + "consciousness\n"
                            + "constellation\n"
                            + "constantly\n"
                            + "constructive\n"
                            + "constraint\n"
                            + "contact\n"
                            + "continue\n"
                            + "contract\n"
                            + "contrast\n"
                            + "control\n"
                            + "consequence\n"
                            + "consciousness\n"
                            + "constellation\n"
                            + "constantly\n"
                            + "constructive\n"
                            + "constraint\n"
                            + "contact\n"
                            + "continue\n"
                            + "contract\n"
                            + "contrast\n"
                            + "control\n"
                            + "consequence\n"
                            + "consciousness\n"
                            + "constellation\n"
                            + "constantly\n"
                            + "constructive\n"
                            + "constraint\n"
                            + "contact\n"
                            + "continue\n"
                            + "contract\n"
                            + "contrast\n"
                            + "control\n"
                            + "consequence\n"
                            + "consciousness\n"
                            + "constellation\n"
                            + "constantly\n"
                            + "constructive\n"
                            + "constraint\n"
                            + "contact\n"
                            + "continue\n"
                            + "contract\n"
                            + "contrast\n"
                            + "control\n"
                            + "consequence\n"
                            + "consciousness\n"
                            + "constellation\n"
                            + "constantly\n"
                            + "constructive\n"
                            + "constraint\n"
                            + "contact\n"
                            + "continue\n"
                            + "contract\n"
                            + "contrast\n"
                            + "control\n"
                            + "consequence\n"
                            + "consciousness\n"
                            + "constellation\n"
                            + "constantly\n"
                            + "constructive\n"
                            + "constraint\n"
                            + "contact\n"
                            + "continue\n"
                            + "contract\n"
                            + "contrast\n"
                            + "control\n"
                            + "consequence\n"
                            +
