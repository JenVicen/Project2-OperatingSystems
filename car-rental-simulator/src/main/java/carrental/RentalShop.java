/*
By Jennifer Vicentes
Purpose: This class represents a rental shop in the car rental system. It contains information about the shop's location, available parking spaces, associated lots, and vehicles in inventory.
It provides methods to rent and return vehicles, list the shop's state, and manage transactions. The class also handles file operations for saving and loading the shop's state.
It uses serialization to save the shop's state to a binary file and a human-readable text file. The class also manages vehicle requests and returns to associated lots using file locking for concurrency safety.
It includes methods to read and write lot files with file locking to ensure that multiple processes do not interfere with each other when accessing the same file.
This class is important for managing the rental shop's operations, including vehicle rentals, returns, and transactions.
It also provides a command-line interface for users to interact with the rental shop, allowing them to rent and return vehicles, list the shop's state, and view transactions.
This class is also responsible for maintaining the shop's state and ensuring that the data is consistent and up-to-date.

Acclaimed AI-generated method: syncWithGlobalRegistryOnStartup()
All the other comments I wrote were put for me to keep track while developing, they are not AI generated.
*/
package carrental;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;

public class RentalShop {
    // Shop state fields
    private String location;
    private int spacesAvailable;
    private List<String> lotNames;
    // Vehicles in shop inventory (available for rental)
    private Map<String, Vehicle> shopInventory = new HashMap<>();
    // Vehicles currently rented out: stored with discount info.
    private Map<String, RentedRecord> rentedVehicles = new HashMap<>();
    // Transaction log
    private List<Transaction> transactions = new ArrayList<>();
    // Shop cash
    private double cashEarned = 0.0;
    // File to persist shop state
    private String shopStateBin;   // e.g. "SanJose.ser"
    private String shopStateTxt;   // e.g. "SanJose.txt"
    private static final String RENTED_REGISTRY = "rented_registry.txt";

    public String executeCommand(String command) {
        // Capture System.out output, redirecting to a ByteArrayOutputStream, this is useful for testing purposes, as we can check the output of the commands
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Redirect System.out to the ByteArrayOutputStream, this allows us to capture the output of the commands and return it as a string
        PrintStream  ps   = new PrintStream(baos);
        // Save the old System.out to restore it later
        PrintStream  old  = System.out;
        System.setOut(ps);

        // Process the command
        processCommand(command);
        saveState();

        // Restores System.out and returns the captured output, what flush does is to ensure that all the data in the stream is written out
        // This is important to ensure that all the data is written out before we return it
        System.out.flush();
        // Restore the old System.out
        System.setOut(old);
        // Convert the ByteArrayOutputStream to a string and return it
        return baos.toString();
    }
    
    // Inner class to store rental record (vehicle + discount flag)
    private static class RentedRecord implements Serializable {
        // Serial version UID for serialization, this means that the class can be serialized and deserialized
        private static final long serialVersionUID = 1L;
        // Vehicle object and a flag indicating if the vehicle came from a lot (thus discount applies)
        Vehicle vehicle;
        boolean discountApplied; // true if vehicle came from a lot (thus discount applies)
        public RentedRecord(Vehicle vehicle, boolean discountApplied){
            this.vehicle = vehicle;
            this.discountApplied = discountApplied;
        }
    }

    // Synchronize the rented vehicles with the global registry on startup
    /* 
    AI Prompt: "How can I ensure that different processes do not interfere, maybe by using File locking please explain it and give me an example/hint."
    Detailed Explanation: This AI-generated prompt gave me this implementation of syncWithGlobalRegistryOnStartup which ensures 
    that multiple processes accessing the same file (RENTED_REGISTRY) do not interfere with each other. 
    File locking is used to prevent concurrent modifications or reads while one process is working with the file. 
    The FileChannel's lock() method acquires a shared lock (read-only) or an exclusive lock (write) on the file. 
    In this case, a shared lock is used (`lock(0L, Long.MAX_VALUE, true)`) to safely read the file without allowing other processes to write to it simultaneously. 
    The method reads the global registry file to synchronize the shop's rented vehicles with the global state, ensuring consistency. 
    Any rented vehicles in the shop that are not present in the global registry are removed to avoid discrepancies.
    */
    private void syncWithGlobalRegistryOnStartup() { // AI generated
        Set<String> globalPlates = new HashSet<>(); // AI generated
        File file = new File(RENTED_REGISTRY); // AI generated
        if (file.exists()) { // AI generated
            try (RandomAccessFile raf = new RandomAccessFile(file, "r"); // AI generated
                FileChannel ch = raf.getChannel(); // AI generated
                FileLock lock = ch.lock(0L, Long.MAX_VALUE, true)) { // AI generated
                String line; // AI generated
                while ((line = raf.readLine()) != null) { // AI generated
                    String[] parts = line.split(","); // AI generated
                    if (parts.length>=1) globalPlates.add(parts[0]); // AI generated
                }
            } catch (IOException e) { // AI generated
                System.err.println("Error reading global registry: " + e.getMessage()); // AI generated
            }
        }
        rentedVehicles.keySet().removeIf(plate -> !globalPlates.contains(plate)); // AI generated
    }
    
    // The constructor initializes the rental shop with a location, available spaces, and associated lots.
    public RentalShop(String location, int spacesAvailable, List<String> lotNames) {
        // Validate input
        this.location = location;
        this.spacesAvailable = spacesAvailable;
        this.lotNames = lotNames;
        // Set file names for binary and text state
        // The .ser file is used for binary serialization, while the .txt file is a human-readable format
        this.shopStateBin = location + ".ser";
        this.shopStateTxt = location + ".txt";
        
        // Load shop state if it exists; otherwise, initialize.
        if (new File(shopStateTxt).exists()) {
            // Load the text state to initialize the shop
            System.out.println("Found existing text state " + shopStateTxt + ", loading binary snapshot.");
            // Initialize the shop with the loaded state
            loadState();
            // Synchronize the rented vehicles with the global registry
            syncWithGlobalRegistryOnStartup();
            writeHumanState();
        } else {
            // Initialize the shop with the provided location, spaces, and lots
            initializeInventory();
        }
    }
    
    // If no vehicle in shop, try to load one from lots.
    private void initializeInventory(){
        if(shopInventory.isEmpty()){
            for(String type: new String[]{"SEDAN", "SUV", "VAN"}){
                // Request a vehicle from the lots
                Vehicle v = requestVehicleFromLots(type);
                // If a vehicle is found, add it to the shop inventory
                if(v != null){
                    // Add the vehicle to the shop inventory
                    shopInventory.put(v.getLicensePlate(), v);
                    // Add the vehicle to the global registry
                    System.out.println("Initialized shop with vehicle " + v.getLicensePlate() + " (" + v.getType() + ")");
                    break;
                }
            }
        }
    }
    
    public static void main(String[] args) {
        Map<String, String> flags = parseArgs(args);
        String loc = flags.get("--location");
        if (loc == null) {
           System.err.println("Error: --location must be provided."); System.exit(1);
        }
    
        // We only look at --spaces-available and --lots when there is NO shopStateTxt:
        if (new File(loc + ".txt").exists()) {
            // If the shop state file exists, we load the state and run the command loop
            RentalShop shop = new RentalShop(loc, 0, List.of());
            shop.runCommandLoop();
        } else {
            // If the shop state file does not exist, we create a new shop with the provided location, spaces, and lots
            int spaces = Integer.parseInt(flags.getOrDefault("--spaces-available", "10"));
            List<String> lots = Arrays.asList(flags.getOrDefault("--lots","").split(","));
            RentalShop shop = new RentalShop(loc, spaces, lots);
            shop.runCommandLoop();
        }
    }
    
    // Add a rented vehicle to the global registry.
    private void addToGlobalRegistry(String plate, String type, boolean discount) {
        // Append the vehicle to the rented registry file with a lock
        File file = new File(RENTED_REGISTRY);
        // Access the file with a lock to ensure no other process is writing to it at the same time
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
            // Acquire a lock on the file to prevent concurrent writes
            FileChannel ch = raf.getChannel();
            FileLock lock = ch.lock()) {
            // Move to the end of the file to append the new vehicle
            raf.seek(raf.length());
            // Write the vehicle information to the file
            raf.writeBytes(String.format("%s,%s,%b%n", plate, type, discount));
        } catch (IOException e) {
            System.err.println("Error writing to rented registry: " + e.getMessage());
        }
    }

    /** Search and remove a rented vehicle from the global registry.
    @return a RentedRecord if it was found or null if it is not registeredx */
    private RentedRecord fetchFromGlobalRegistry(String plate) {
        File file = new File(RENTED_REGISTRY);
        // Check if the file exists before trying to read it
        if (!file.exists()) return null;
        List<String> lines = new ArrayList<>();
        RentedRecord found = null;
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel ch = raf.getChannel();
            FileLock lock = ch.lock()) {
            // Move to the beginning of the file to read all lines
            raf.seek(0);
            String line;
            // Read each line and check if the vehicle is found
            while ((line = raf.readLine()) != null) {
                String[] parts = line.split(",");
                // Check if the line has enough parts to avoid ArrayIndexOutOfBoundsException
                if (parts[0].equals(plate) && found == null) {
                    // If the vehicle is found, create a RentedRecord and set it to found
                    boolean discount = Boolean.parseBoolean(parts[2]);
                    found = new RentedRecord(new Vehicle(plate, parts[1], 0), discount);
                } else {
                    lines.add(line);
                }
            }
            // rewrite the file without the found vehicle
            raf.setLength(0);
            for (String l : lines) raf.writeBytes(l + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Error reading rented registry: " + e.getMessage());
        }
        return found;
    }
    
    // Interactive command loop.
    private void runCommandLoop(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Rental Shop at " + location + " ready. Type commands (RENT, RETURN, LIST, TRANSACTIONS). Type EXIT to quit.");
        while(true){
            System.out.print("> ");
            String input = scanner.nextLine();
            if(input == null || input.trim().equalsIgnoreCase("EXIT")){
                break;
            }
            processCommand(input.trim());
            saveState();
        }
        scanner.close();
    }
    
    // Process one command.
    private void processCommand(String input){
        // Split the input into tokens
        String[] tokens = input.split("\\s+");
        if(tokens.length == 0) return;
        String command = tokens[0].toUpperCase();
        switch(command){
            case "RENT":
                if(tokens.length < 2){
                    System.out.println("Usage: RENT <VEHICLE TYPE>");
                } else {
                    rentVehicle(tokens[1].toUpperCase());
                }
                break;
            case "RETURN":
                if(tokens.length < 3){
                    System.out.println("Usage: RETURN <LICENSE PLATE> <KILOMETERS>");
                } else {
                    try {
                        int km = Integer.parseInt(tokens[2]);
                        returnVehicle(tokens[1].toUpperCase(), km);
                    } catch(NumberFormatException e){
                        System.out.println("Invalid kilometers value.");
                    }
                }
                break;
            case "LIST":
                listShopState();
                break;
            case "TRANSACTIONS":
                listTransactions();
                break;
            default:
                System.out.println("Unknown command.");
        }
    }
    
    // RENT command: check for available vehicle or request one from lots.
    private void rentVehicle(String vehicleType){
        int currentEmptySlots = spacesAvailable - (shopInventory.size() + rentedVehicles.size());
        while (currentEmptySlots == 0) { // Block if no empty slots
            System.out.println("Cannot rent more vehicles because there are no empty slots!");
            return;
        }

        Vehicle vehicle = null;
        // Look in shop inventory.
        for(Vehicle v : shopInventory.values()){
            if(v.getType().equalsIgnoreCase(vehicleType)){
                vehicle = v;
                break;
            }
        }
        boolean discountApplied = false;
        if(vehicle != null){
            // Vehicle found in shop inventory.
            // Remove it from the inventory.
            shopInventory.remove(vehicle.getLicensePlate());
            System.out.println("RENT: Provided vehicle " + vehicle.getLicensePlate() + " (" + vehicle.getType() + ") from shop inventory.");
        } else {
            // Request from lots.
            vehicle = requestVehicleFromLots(vehicleType);
            if(vehicle != null){
                discountApplied = true;
                System.out.println("RENT: Obtained vehicle " + vehicle.getLicensePlate() + " (" + vehicle.getType() + ") from lot with 10% discount.");
            } else {
                System.out.println("RENT: No available vehicle of type " + vehicleType + " in shop or lots.");
                return;
            }
        }
        // Record the rented vehicle.
        rentedVehicles.put(vehicle.getLicensePlate(), new RentedRecord(vehicle, discountApplied));
        // Add the rented vehicle to the global registry.
        addToGlobalRegistry(vehicle.getLicensePlate(), vehicle.getType(), discountApplied);
    }
    
    // RETURN command: update kilometers, compute charge, and update shop cash.
    private void returnVehicle(String licensePlate, int kilometers){
        // Check if the vehicle is rented from this shop.
        RentedRecord record = rentedVehicles.remove(licensePlate);
        if (record == null) {
            // If not found, check the global registry.
            record = fetchFromGlobalRegistry(licensePlate);
            if (record == null) {
                System.out.println("RETURN: Vehicle " + licensePlate + " is not rented by any shop.");
                return;
            }
        }
        Vehicle vehicle = record.vehicle;
        vehicle.addKilometers(kilometers);
        // Compute charge: $1 per km, discount applies if vehicle came from a lot.
        double charge = kilometers;
        // If a discount was applied, reduce the charge by 10%.
        if(record.discountApplied){
            double discount = 0.10 * charge;
            charge -= discount;
        }
        cashEarned += charge;
        transactions.add(new Transaction(licensePlate, kilometers, record.discountApplied, charge));
        System.out.println("RETURN: Vehicle " + licensePlate + " returned. Km added: " + kilometers + ". Charge: $" + charge);
        
        // Check parking: if after return the empty spots fall below 2, move one vehicle back to a lot.
        int totalVehicles = shopInventory.size() + rentedVehicles.size();
        int emptySpots = spacesAvailable - totalVehicles;
        if(emptySpots < 2 && !shopInventory.isEmpty()){
            // Select the vehicle with the highest kilometers.
            Vehicle toMove = Collections.max(shopInventory.values(), Comparator.comparingInt(Vehicle::getKilometers));
            shopInventory.remove(toMove.getLicensePlate());
            String targetLot = lotNames.get(0);  // For simplicity, return to the first lot.
            returnVehicleToLot(toMove, targetLot);
            System.out.println("RETURN: Moved vehicle " + toMove.getLicensePlate() + " (" + toMove.getType() + ") to lot " + targetLot + " due to low parking space.");
        }
        // Finally, add the returned vehicle to the shop inventory.
        shopInventory.put(licensePlate, vehicle);
        rentedVehicles.remove(licensePlate);
    }
    
    // LIST command: display shop state.
    private void listShopState(){
        System.out.println("----- Shop State (" + location + ") -----");
        System.out.println("Parking Spaces Available: " + (spacesAvailable - (shopInventory.size() + rentedVehicles.size())));
        System.out.println("Vehicles in Shop Inventory:");
        for(Vehicle v : shopInventory.values()){
            System.out.println("  " + v);
        }
        System.out.println("Vehicles Rented Out:");
        for(RentedRecord r : rentedVehicles.values()){
            System.out.println("  " + r.vehicle);
        }
        System.out.println("Cash Earned: $" + cashEarned);
    }
    
    // TRANSACTIONS command: list all return transactions and totals.
    private void listTransactions(){
        System.out.println("----- Transaction Log (" + location + ") -----");
        double totalDiscount = 0;
        for(Transaction t : transactions){
            System.out.println(t);
            if(t.isDiscountApplied()){
                // Discount amount is implicit in the charge calculation.
                double discount = (0.10 * t.getCharge()) / 0.90;
                totalDiscount += discount;
            }
        }
        System.out.println("Total Earnings: $" + cashEarned);
        System.out.println("Total Lost Due To Discounts: $" + totalDiscount);
    }
    
    // --- Concurrency-Safe Lot File Operations ---
    
    // Request a vehicle from one of the associated lots using file locking.
    private Vehicle requestVehicleFromLots(String vehicleType){
        for(String lotName : lotNames){
            String lotFile = lotName + ".txt";
            List<Vehicle> lotVehicles = readLotFileWithLock(lotFile);
            Iterator<Vehicle> iter = lotVehicles.iterator();
            while(iter.hasNext()){
                Vehicle v = iter.next();
                if(v.getType().equalsIgnoreCase(vehicleType)){
                    // Remove vehicle from list and update the lot file.
                    iter.remove();
                    writeLotFileWithLock(lotFile, lotVehicles);
                    return v;
                }
            }
        }
        return null;
    }
    
    // Return a vehicle back to a specified lot using file locking.
    private void returnVehicleToLot(Vehicle vehicle, String lotName){
        String lotFile = lotName + ".txt";
        List<Vehicle> lotVehicles = readLotFileWithLock(lotFile);
        lotVehicles.add(vehicle);
        writeLotFileWithLock(lotFile, lotVehicles);
    }
    
    // Reads a lot file using file locking.
    private List<Vehicle> readLotFileWithLock(String fileName){
        List<Vehicle> vehicles = new ArrayList<>();
        File file = new File(fileName);
        if(!file.exists()){
            return vehicles;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel channel = raf.getChannel();
             FileLock lock = channel.lock(0L, Long.MAX_VALUE, true)) {
             
            raf.seek(0);
            String line;
            while((line = raf.readLine()) != null){
                String[] parts = line.split(",");
                if(parts.length >= 3){
                    String plate = parts[0].trim();
                    String type = parts[1].trim();
                    int km = Integer.parseInt(parts[2].trim());
                    vehicles.add(new Vehicle(plate, type, km));
                }
            }
        } catch(IOException e){
            System.err.println("Error reading lot file (" + fileName + "): " + e.getMessage());
        }
        return vehicles;
    }
    
    // Writes a lot file using file locking.
    private void writeLotFileWithLock(String fileName, List<Vehicle> vehicles){
        File file = new File(fileName);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel channel = raf.getChannel();
             FileLock lock = channel.lock()) {
             
            // Overwrite the file contents.
            raf.setLength(0);
            for(Vehicle v : vehicles){
                raf.writeBytes(v.getLicensePlate() + "," + v.getType() + "," + v.getKilometers() + System.lineSeparator());
            }
        } catch(IOException e){
            System.err.println("Error writing lot file (" + fileName + "): " + e.getMessage());
        }
    }
    
    // Save the shop state using serialization.
    private void saveState() {
        // Save the shop state to a binary file and a human-readable text file.
        saveBinaryState();
        writeHumanState();
    }
    
    // 1) Binary snapshot for fast reload:
    private void saveBinaryState() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(shopStateBin))) {
            // Serialize the shop state
            oos.writeObject(shopInventory);
            oos.writeObject(rentedVehicles);
            oos.writeObject(transactions);
            oos.writeDouble(cashEarned);
            oos.writeInt(spacesAvailable);
            oos.writeObject(lotNames);
        } catch (IOException e) {
            System.err.println("Error saving binary state: " + e.getMessage());
        }
    }
    
    // 2) Human‐readable dump for city.txt:
    private void writeHumanState() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(shopStateTxt))) {
            // Headers
            pw.println("LOCATION: " + location);
            pw.println("SPACES_AVAILABLE: " + spacesAvailable);
            int empty = spacesAvailable - (shopInventory.size() + rentedVehicles.size());
            pw.println("EMPTY_SLOTS: " + empty);
            pw.println("CASH_EARNED: $" + String.format("%.2f", cashEarned));
            pw.println();
    
            // Inventory
            pw.println("INVENTORY:");
            for (Vehicle v : shopInventory.values()) {
                pw.printf("  %s,%s,%d%n",
                    v.getLicensePlate(),
                    v.getType(),
                    v.getKilometers());
            }
            pw.println();
    
            // Rented vehicles
            pw.println("RENTED_OUT:");
            for (RentedRecord r : rentedVehicles.values()) {
                pw.printf("  %s,%s,%d,discount=%s%n",
                    r.vehicle.getLicensePlate(),
                    r.vehicle.getType(),
                    r.vehicle.getKilometers(),
                    r.discountApplied ? "10%" : "0%");
            }
            pw.println();
    
            // Transactions
            pw.println("TRANSACTIONS:");
            for (Transaction t : transactions) {
                pw.printf("  %s,%d,discount=%s,$%.2f%n",
                    t.getLicensePlate(),
                    t.getKilometers(),
                    t.isDiscountApplied() ? "10%" : "0%",
                    t.getCharge());
            }
        } catch (IOException e) {
            System.err.println("Error writing human state: " + e.getMessage());
        }
    }
    
    // Load the shop state from file.
    // This method is used to load the shop state from a binary file.
    // It reads the serialized objects from the file and assigns them to the corresponding fields in the RentalShop class.
    // The method uses ObjectInputStream to read the objects from the file and cast them to the appropriate types.
    // The method also handles exceptions that may occur during the loading process, such as FileNotFoundException or ClassNotFoundException.
    @SuppressWarnings("unchecked")
    private void loadState() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(shopStateBin))) {
            shopInventory   = (Map<String, Vehicle>)     ois.readObject();
            rentedVehicles  = (Map<String, RentedRecord>)ois.readObject();
            transactions    = (List<Transaction>)        ois.readObject();
            cashEarned      = ois.readDouble();
            spacesAvailable = ois.readInt();
            lotNames        = (List<String>)             ois.readObject();
        } catch (Exception e) {
            System.err.println("Error loading binary state: " + e.getMessage());
        }
    }
    
    // Parse command line arguments.
    public static Map<String,String> parseArgs(String[] args){
        Map<String, String> flags = new HashMap<>();
        for(String arg: args){
            if(arg.startsWith("--")){
                int eq = arg.indexOf('=');
                if(eq > 0){
                    String key = arg.substring(0, eq);
                    String value = arg.substring(eq + 1);
                    flags.put(key, value);
                } else {
                    flags.put(arg, "");
                }
            }
        }
        return flags;
    }
}

