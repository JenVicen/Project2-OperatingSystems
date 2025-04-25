/* 
By Jennifer Vicentes

AI Prompt: "Help me creating a base for a RentalShop and LotManager UI in Java. It should be basic like a good starting point.
Also explain me every library and method you are using, so I can understand what is going on.
I want to be able to represent the LotManager and the RentalShop classes."

Detailed Explanation: This code is a Java Swing application that provides a graphical user interface (GUI) for a car rental system.
It allows users to manage rental lots and a rental shop, including adding vehicles, renting them out, and returning them. The code is structured into two main tabs: Lot Manager and Rental Shop.
The Lot Manager tab allows users to add vehicles to a lot, remove them, and refresh the lot's contents. The Rental Shop tab allows users to initialize a rental shop, rent vehicles, return them, and view transactions.
The code uses the following libraries:
1. javax.swing: This library provides classes for creating GUI components such as buttons, text fields, and panels. It is part of the Java Foundation Classes (JFC) and is used to create window-based applications.
2. java.awt: This library provides classes for creating user interfaces and for painting graphics and images. It includes classes for layout managers, colors, fonts, and event handling.
3. java.io: This library provides classes for input and output through data streams, serialization, and file handling. It is used to read and write files, which is essential for saving and loading the state of the rental lots and transactions.
4. java.util: This library provides utility classes such as collections, date and time facilities, and random number generation. It is used for data structures like lists and arrays, which are essential for managing the rental lots and vehicles.
*/

package carrental;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class CarRentalUI extends JFrame {
    // --- Lot Manager fields ---
    private JTextField lotNameField; // AI Generated
    private JSpinner   sedanSpinner, suvSpinner, vanSpinner; // AI Generated
    private JButton    addSedanBtn, addSuvBtn, addVanBtn, removeVehicleBtn, refreshLotBtn; // AI Generated
    private JTextArea  lotOutputArea; // AI Generated

    // --- Rental Shop fields ---
    private JTextField               shopLocationField; // AI Generated
    private JSpinner                 spacesSpinner; // AI Generated
    private DefaultListModel<String> lotListModel; // AI Generated
    private JList<String>            lotsList; // AI Generated
    private JButton                  refreshLotsBtn, initShopBtn; // AI Generated
    private JComboBox<String>        typeCombo; // AI Generated
    private JButton                  rentBtn, returnBtn, listBtn, txBtn; // AI Generated
    private JTextField               plateField, kmField; // AI Generated
    private JTextArea                shopOutputArea; // AI Generated

    private RentalShop shop;

    public CarRentalUI() {
        super("Car Rental Simulator");
        setLayout(new BorderLayout()); // AI Generated

        JTabbedPane tabs = new JTabbedPane(); // AI Generated
        tabs.addTab("Lot Manager", buildLotManagerPanel()); // AI Generated
        tabs.addTab("Rental Shop",  buildShopPanel()); // AI Generated
        add(tabs, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE); // AI Generated
        pack(); // AI Generated
        setLocationRelativeTo(null); // AI Generated
        setVisible(true); // AI Generated

        // Initial population of the lots list
        refreshLotsList();
    }

    // --------------------------------------
    // Lot Manager Tab
    // --------------------------------------
    private JPanel buildLotManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(5,5));

        // Top row of controls
        // This is where the user can enter the lot name and add vehicles
        // It uses a FlowLayout to arrange the components in a single row
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lotNameField     = new JTextField(10); // AI Generated
        sedanSpinner     = new JSpinner(new SpinnerNumberModel(1,1,100,1)); // AI Generated
        addSedanBtn      = new JButton("Add Sedan"); // AI Generated
        suvSpinner       = new JSpinner(new SpinnerNumberModel(1,1,100,1)); // AI Generated
        addSuvBtn        = new JButton("Add SUV");
        vanSpinner       = new JSpinner(new SpinnerNumberModel(1,1,100,1));
        addVanBtn        = new JButton("Add Van");
        removeVehicleBtn = new JButton("Remove Vehicle");   // AI Generated
        refreshLotBtn    = new JButton("Refresh Lot");

        top.add(new JLabel("Lot Name:")); top.add(lotNameField);
        top.add(new JLabel("Sedans:"));   top.add(sedanSpinner); top.add(addSedanBtn);
        top.add(new JLabel("SUVs:"));     top.add(suvSpinner);   top.add(addSuvBtn);
        top.add(new JLabel("Vans:"));     top.add(vanSpinner);   top.add(addVanBtn);
        top.add(removeVehicleBtn);        top.add(refreshLotBtn);

        panel.add(top, BorderLayout.NORTH); // AI Generated

        // Center: output area
        lotOutputArea = new JTextArea(15, 60); // AI Generated
        lotOutputArea.setEditable(false);   // AI Generated
        panel.add(new JScrollPane(lotOutputArea), BorderLayout.CENTER); // AI Generated

        // Hook up actions
        addSedanBtn     .addActionListener(e -> runLotCommand("--add-sedan"));
        addSuvBtn       .addActionListener(e -> runLotCommand("--add-suv"));
        addVanBtn       .addActionListener(e -> runLotCommand("--add-van"));
        removeVehicleBtn.addActionListener(e -> runLotCommand("--remove-vehicle"));
        refreshLotBtn   .addActionListener(e -> runLotCommand("--refresh"));

        return panel;
    }

    /**
     * Executes a LotManager command and displays both console output and file contents.
     */
    private void runLotCommand(String flag) {
        String lot = lotNameField.getText().trim();
        if(lot.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lot name is required");
            return;
        }

        // Build arguments
        List<String> args = new ArrayList<>();
        args.add("--lot-name=" + lot);
        switch(flag) {
            case "--add-sedan":
                args.add("--add-sedan=" + sedanSpinner.getValue());
                break;
            case "--add-suv":
                args.add("--add-suv=" + suvSpinner.getValue());
                break;
            case "--add-van":
                args.add("--add-van=" + vanSpinner.getValue());
                break;
            case "--remove-vehicle":
                String plate = JOptionPane.showInputDialog(this, "Enter license plate to remove:");
                if(plate == null || plate.trim().isEmpty()) return;
                args.add("--remove-vehicle=" + plate.trim());
                break;
            case "--refresh":
                // no additional flag
                break;
        }

        // Capture System.out from LotManager
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos), old = System.out;
        System.setOut(ps);
        LotManager.main(args.toArray(new String[0]));
        System.out.flush();
        System.setOut(old);

        // Show console output
        lotOutputArea.append(baos.toString() + "\n");

        // Show updated lot file
        File f = new File(lot + ".txt");
        if(f.exists()) {
            lotOutputArea.append("Contents of " + f.getName() + ":\n");
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lotOutputArea.append("  " + line + "\n");
                }
            } catch (IOException ex) {
                lotOutputArea.append("  (error reading file)\n");
            }
            lotOutputArea.append("\n");
        }

        // Refresh the Rental Shop's lot list too
        refreshLotsList();
    }

    // --------------------------------------
    // Rental Shop Tab
    // --------------------------------------
    private JPanel buildShopPanel() {
        JPanel panel = new JPanel(new BorderLayout(5,5)); // AI Generated

        // Top configuration row
        JPanel config = new JPanel(new FlowLayout(FlowLayout.LEFT)); // AI Generated
        shopLocationField = new JTextField(8); // AI Generated
        spacesSpinner     = new JSpinner(new SpinnerNumberModel(5,1,100,1)); // AI Generated

        // The lots list model and JList
        lotListModel = new DefaultListModel<>(); // AI Generated
        lotsList     = new JList<>(lotListModel); // AI Generated
        lotsList.setVisibleRowCount(4); // AI Generated
        lotsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // AI Generated
        JScrollPane listScroll = new JScrollPane(lotsList); // AI Generated
        listScroll.setPreferredSize(new Dimension(120,80)); // AI Generated

        refreshLotsBtn = new JButton("Refresh Lots"); // AI Generated
        initShopBtn    = new JButton("Initialize Shop"); // AI Generated

        config.add(new JLabel("Location:")); config.add(shopLocationField); // AI Generated
        config.add(new JLabel("Spaces:"));   config.add(spacesSpinner); // AI Generated
        config.add(new JLabel("Lots:"));     config.add(listScroll); // AI Generated
        config.add(refreshLotsBtn);          config.add(initShopBtn); // AI Generated

        panel.add(config, BorderLayout.NORTH); // AI Generated

        // Center: output area
        shopOutputArea = new JTextArea(15, 60);
        shopOutputArea.setEditable(false);
        panel.add(new JScrollPane(shopOutputArea), BorderLayout.CENTER);

        // Bottom: action buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typeCombo  = new JComboBox<>(new String[]{"SEDAN","SUV","VAN"});
        rentBtn    = new JButton("RENT");
        plateField = new JTextField(8);
        kmField    = new JTextField(4);
        returnBtn  = new JButton("RETURN");
        listBtn    = new JButton("LIST");
        txBtn      = new JButton("TRANSACTIONS");

        bottom.add(new JLabel("Type:"));    bottom.add(typeCombo); bottom.add(rentBtn);
        bottom.add(new JLabel("Plate:"));   bottom.add(plateField);
        bottom.add(new JLabel("Km:"));      bottom.add(kmField);    bottom.add(returnBtn);
        bottom.add(listBtn);                bottom.add(txBtn);

        panel.add(bottom, BorderLayout.SOUTH);

        // Listeners
        refreshLotsBtn.addActionListener(e -> refreshLotsList());
        initShopBtn   .addActionListener(e -> initializeShop());
        rentBtn       .addActionListener(e -> shopCommand("RENT "        + typeCombo.getSelectedItem()));
        returnBtn     .addActionListener(e -> shopCommand("RETURN "      + plateField.getText().trim() 
                                                                   + " " + kmField.getText().trim()));
        listBtn       .addActionListener(e -> shopCommand("LIST"));
        txBtn         .addActionListener(e -> shopCommand("TRANSACTIONS"));

        return panel;
    }

    // Re‐scans the current directory for "*.txt" files that are real lots,
    // excluding the global registries and shop state files.
    // Preserves the user’s current selection if still present.
    private void refreshLotsList() {
        // Save old selection
        List<String> oldSelection = lotsList.getSelectedValuesList();

        // Rebuild list model
        lotListModel.clear();
        File cwd = new File(".");
        String[] files = cwd.list((d,n) ->
            n.toLowerCase().endsWith(".txt")
            && !n.equalsIgnoreCase("plates_registry.txt")
            && !n.equalsIgnoreCase("rented_registry.txt")
            && !n.equalsIgnoreCase(shopLocationField.getText().trim()+".txt")
        );
        if(files != null) {
            Arrays.sort(files);
            for(String fn : files) {
                lotListModel.addElement(fn.substring(0, fn.length()-4));
            }
        }

        // Restore selection
        // We clear the selection and then reselect the old values
        // This is important to ensure that the user’s selection is preserved
        // even if the list model has changed
        lotsList.clearSelection();
        for(String s : oldSelection) {
            int idx = lotListModel.indexOf(s);
            if(idx >= 0) lotsList.addSelectionInterval(idx, idx);
        }
    }

    // Initializes or reloads the RentalShop instance using the selected lots.
    private void initializeShop() {
        String loc = shopLocationField.getText().trim();
        if(loc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a shop location");
            return;
        }
        List<String> selectedLots = lotsList.getSelectedValuesList();
        if(selectedLots.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one lot");
            return;
        }
        int spaces = (Integer)spacesSpinner.getValue();
        shop = new RentalShop(loc, spaces, selectedLots);
        shopOutputArea.append("Initialized shop at " + loc + "\n\n");
    }

    /** Sends a single command to the shop and appends its output. */
    private void shopCommand(String cmd) {
        if(shop == null) {
            JOptionPane.showMessageDialog(this, "Initialize the shop first");
            return;
        }
        String out = shop.executeCommand(cmd);
        shopOutputArea.append(out + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CarRentalUI::new); // AI Generated
    }
}