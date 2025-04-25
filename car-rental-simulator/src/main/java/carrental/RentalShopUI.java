/* 
By Jennifer Vicentes

AI Prompt: "Help me creating a base for a RentalShop UI in Java. It should be basic like a good starting point.
Also explain me every library and method you are using, so I can understand what is going on.
I want to be able to rent a car, return a car, list all cars and list all transactions."

Detailed Explanation: This class represents the user interface for the car rental shop. It uses Swing, 
a part of Java's standard library for creating graphical user interfaces (GUIs). The UI allows users to 
interact with the rental shop, rent and return vehicles, and view transactions.
It includes a main method to run the UI, which is created in the Event Dispatch Thread (EDT) to ensure thread safety. 
The UI consists of various components such as buttons, text fields, and labels arranged in panels using layout managers. 
The action listeners handle user interactions and execute commands on the rental shop instance.
This is important for creating a responsive and interactive application.
*/

package carrental;

import javax.swing.*; // AI generated
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RentalShopUI extends JFrame { // AI generated
    private RentalShop shop;
    private JComboBox<String> typeCombo; // AI generated
    private JTextField plateField, kmField; // AI generated
    private JTextArea outputArea; // AI generated

    public RentalShopUI(String location, int spaces, List<String> lots) {
        super("Rental Shop - " + location);
        shop = new RentalShop(location, spaces, lots);

        // Principal Layout
        setLayout(new BorderLayout()); // AI generated

        // Rent and Return Panels
        JPanel top = new JPanel(new GridLayout(2,1,0,5)); // AI generated
        // RENT
        JPanel rentPanel = new JPanel(); // AI generated
        typeCombo = new JComboBox<>(new String[]{"SEDAN","SUV","VAN"}); // AI generated
        JButton rentBtn = new JButton("RENT"); // AI generated
        rentPanel.add(new JLabel("Type:")); // AI generated
        rentPanel.add(typeCombo); // AI generated
        rentPanel.add(rentBtn); // AI generated
        top.add(rentPanel); // AI generated

        // RETURN
        JPanel retPanel = new JPanel();
        plateField = new JTextField(8);
        kmField    = new JTextField(4);
        JButton returnBtn = new JButton("RETURN");
        retPanel.add(new JLabel("Plate:")); // AI generated
        retPanel.add(plateField); // AI generated
        retPanel.add(new JLabel("Km:")); // AI generated
        retPanel.add(kmField); // AI generated
        retPanel.add(returnBtn); // AI generated
        top.add(retPanel); // AI generated

        add(top, BorderLayout.NORTH); // AI generated

        // Center Panel: Output Area
        outputArea = new JTextArea(15, 50); // AI generated
        outputArea.setEditable(false); // AI generated
        add(new JScrollPane(outputArea), BorderLayout.CENTER); // AI generated

        // List and Transactions Buttons
        JPanel bottom = new JPanel();
        JButton listBtn = new JButton("LIST");
        JButton txBtn   = new JButton("TRANSACTIONS");
        bottom.add(listBtn);
        bottom.add(txBtn);
        add(bottom, BorderLayout.SOUTH); // AI generated

        // Listeners
        rentBtn.addActionListener(e -> { // AI generated
            String res = shop.executeCommand("RENT " + typeCombo.getSelectedItem()); // AI generated
            outputArea.append(res); // AI generated
        });
        returnBtn.addActionListener(e -> {
            String plate = plateField.getText().trim(); // AI generated
            String kms   = kmField.getText().trim();
            String res   = shop.executeCommand("RETURN " + plate + " " + kms);
            outputArea.append(res);
        });
        listBtn.addActionListener(e -> {
            String res = shop.executeCommand("LIST");
            outputArea.append(res);
        });
        txBtn.addActionListener(e -> {
            String res = shop.executeCommand("TRANSACTIONS");
            outputArea.append(res);
        });

        // Window settings
        pack(); // AI generated
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // AI generated
        setLocationRelativeTo(null); // AI generated
        setVisible(true); // AI generated
    }

    public static void main(String[] args) {
        // Reuse the command line argument parsing from RentalShop
        Map<String,String> flags = carrental.RentalShop.parseArgs(args);
        String location    = flags.get("--location");
        if (location == null) {
            JOptionPane.showMessageDialog(null, "Error: --location must be provided.","Error",JOptionPane.ERROR_MESSAGE); // AI generated
            System.exit(1);
        }
        int spaces = Integer.parseInt(flags.getOrDefault("--spaces-available","10"));
        List<String> lots = Arrays.asList(flags.getOrDefault("--lots","").split(","));

        // Run the UI in the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new RentalShopUI(location, spaces, lots)); // AI generated
    }
}
