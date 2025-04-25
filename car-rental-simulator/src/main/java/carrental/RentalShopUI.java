package carrental;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RentalShopUI extends JFrame {
    private RentalShop shop;
    private JComboBox<String> typeCombo;
    private JTextField plateField, kmField;
    private JTextArea outputArea;

    public RentalShopUI(String location, int spaces, List<String> lots) {
        super("Rental Shop - " + location);
        shop = new RentalShop(location, spaces, lots);

        // Principal Layout
        setLayout(new BorderLayout());

        // Rent and Return Panels
        JPanel top = new JPanel(new GridLayout(2,1,0,5));

        // RENT
        JPanel rentPanel = new JPanel();
        typeCombo = new JComboBox<>(new String[]{"SEDAN","SUV","VAN"});
        JButton rentBtn = new JButton("RENT");
        rentPanel.add(new JLabel("Type:"));
        rentPanel.add(typeCombo);
        rentPanel.add(rentBtn);
        top.add(rentPanel);

        // RETURN
        JPanel retPanel = new JPanel();
        plateField = new JTextField(8);
        kmField    = new JTextField(4);
        JButton returnBtn = new JButton("RETURN");
        retPanel.add(new JLabel("Plate:"));
        retPanel.add(plateField);
        retPanel.add(new JLabel("Km:"));
        retPanel.add(kmField);
        retPanel.add(returnBtn);
        top.add(retPanel);

        add(top, BorderLayout.NORTH);

        // Center Panel: Output Area
        outputArea = new JTextArea(15, 50);
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // List and Transactions Buttons
        JPanel bottom = new JPanel();
        JButton listBtn = new JButton("LIST");
        JButton txBtn   = new JButton("TRANSACTIONS");
        bottom.add(listBtn);
        bottom.add(txBtn);
        add(bottom, BorderLayout.SOUTH);

        // Listeners
        rentBtn.addActionListener(e -> {
            String res = shop.executeCommand("RENT " + typeCombo.getSelectedItem());
            outputArea.append(res);
        });
        returnBtn.addActionListener(e -> {
            String plate = plateField.getText().trim();
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
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        // Reuse the command line argument parsing from RentalShop
        Map<String,String> flags = carrental.RentalShop.parseArgs(args);
        String loc    = flags.get("--location");
        if (loc == null) {
            JOptionPane.showMessageDialog(null, "Error: --location must be provided.","Error",JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        int spaces = Integer.parseInt(flags.getOrDefault("--spaces-available","10"));
        List<String> lots = Arrays.asList(flags.getOrDefault("--lots","").split(","));

        // Run the UI in the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new RentalShopUI(loc, spaces, lots));
    }
}
