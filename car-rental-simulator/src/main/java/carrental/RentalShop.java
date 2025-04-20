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
    
    // Inner class to store rental record (vehicle + discount flag)
    private static class RentedRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        Vehicle vehicle;
        boolean discountApplied; // true if vehicle came from a lot (thus discount applies)
        public RentedRecord(Vehicle vehicle, boolean discountApplied){
            this.vehicle = vehicle;
            this.discountApplied = discountApplied;
        }
    }

    private void syncWithGlobalRegistryOnStartup() {
        Set<String> globalPlates = new HashSet<>();
        File file = new File(RENTED_REGISTRY);
        if (file.exists()) {
            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                FileChannel ch = raf.getChannel();
                FileLock lock = ch.lock(0L, Long.MAX_VALUE, true)) {
                String line;
                while ((line = raf.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length>=1) globalPlates.add(parts[0]);
                }
            } catch (IOException e) {
                System.err.println("Error leyendo registro global: " + e.getMessage());
            }
        }
        rentedVehicles.keySet().removeIf(plate -> !globalPlates.contains(plate));
    }
    
    public RentalShop(String location, int spacesAvailable, List<String> lotNames) {
        this.location = location;
        this.spacesAvailable = spacesAvailable;
        this.lotNames = lotNames;
        this.shopStateBin = location + ".ser";
        this.shopStateTxt = location + ".txt";
        
        // Load shop state if it exists; otherwise, initialize.
        if (new File(shopStateTxt).exists()) {
            System.out.println("Found existing text state " + shopStateTxt + ", loading binary snapshot.");
            loadState();
            syncWithGlobalRegistryOnStartup();
            writeHumanState();
        } else {
            initializeInventory();
        }
    }
    
    // If no vehicle in shop, try to load one from lots.
    private void initializeInventory(){
        if(shopInventory.isEmpty()){
            for(String type: new String[]{"SEDAN", "SUV", "VAN"}){
                Vehicle v = requestVehicleFromLots(type);
                if(v != null){
                    shopInventory.put(v.getLicensePlate(), v);
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
            RentalShop shop = new RentalShop(loc, 0, List.of());
            shop.runCommandLoop();
        } else {
            int spaces = Integer.parseInt(flags.getOrDefault("--spaces-available", "10"));
            List<String> lots = Arrays.asList(flags.getOrDefault("--lots","").split(","));
            RentalShop shop = new RentalShop(loc, spaces, lots);
            shop.runCommandLoop();
        }
    }
    
    /** Agrega un vehículo rentado al registro compartido. */
    private void addToGlobalRegistry(String plate, String type, boolean discount) {
        File file = new File(RENTED_REGISTRY);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel ch = raf.getChannel();
            FileLock lock = ch.lock()) {
            raf.seek(raf.length());
            raf.writeBytes(String.format("%s,%s,%b%n", plate, type, discount));
        } catch (IOException e) {
            System.err.println("Error writing to rented registry: " + e.getMessage());
        }
    }

    /** Busca y elimina del registro global un vehículo rentado. 
        @return un RentedRecord si lo encontró, o null si no está registrado */
    private RentedRecord fetchFromGlobalRegistry(String plate) {
        File file = new File(RENTED_REGISTRY);
        if (!file.exists()) return null;
        List<String> lines = new ArrayList<>();
        RentedRecord found = null;
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel ch = raf.getChannel();
            FileLock lock = ch.lock()) {
            raf.seek(0);
            String line;
            while ((line = raf.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(plate) && found == null) {
                    // línea que buscamos: lic, tipo, discount
                    boolean discount = Boolean.parseBoolean(parts[2]);
                    found = new RentedRecord(new Vehicle(plate, parts[1], 0), discount);
                } else {
                    lines.add(line); // lo guardamos para reescribir
                }
            }
            // reescribimos sin la línea extraída
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
        addToGlobalRegistry(vehicle.getLicensePlate(), vehicle.getType(), discountApplied);
    }
    
    // RETURN command: update kilometers, compute charge, and update shop cash.
    private void returnVehicle(String licensePlate, int kilometers){
        RentedRecord record = rentedVehicles.remove(licensePlate);
        if (record == null) {
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
        saveBinaryState();
        writeHumanState();
    }
    
    // 1) Binary snapshot for fast reload:
    private void saveBinaryState() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(shopStateBin))) {
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
            // Encabezado
            pw.println("LOCATION: " + location);
            pw.println("SPACES_AVAILABLE: " + spacesAvailable);
            int empty = spacesAvailable - (shopInventory.size() + rentedVehicles.size());
            pw.println("EMPTY_SLOTS: " + empty);
            pw.println("CASH_EARNED: $" + String.format("%.2f", cashEarned));
            pw.println();
    
            // Inventario
            pw.println("INVENTORY:");
            for (Vehicle v : shopInventory.values()) {
                pw.printf("  %s,%s,%d%n",
                    v.getLicensePlate(),
                    v.getType(),
                    v.getKilometers());
            }
            pw.println();
    
            // Rentados
            pw.println("RENTED_OUT:");
            for (RentedRecord r : rentedVehicles.values()) {
                pw.printf("  %s,%s,%d,discount=%s%n",
                    r.vehicle.getLicensePlate(),
                    r.vehicle.getType(),
                    r.vehicle.getKilometers(),
                    r.discountApplied ? "10%" : "0%");
            }
            pw.println();
    
            // Transacciones
            pw.println("TRANSACTIONS:");
            for (Transaction t : transactions) {
                pw.printf("  %s,%d,discount=%s,$%.2f%n",
                    t.getLicensePlate(),           // String -> %s
                    t.getKilometers(),             // int    -> %d
                    t.isDiscountApplied() ? "10%" : "0%", // String -> %s
                    t.getCharge());                // double -> %.2f
            }
        } catch (IOException e) {
            System.err.println("Error writing human state: " + e.getMessage());
        }
    }
    
    // Load the shop state from file.
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
    private static Map<String,String> parseArgs(String[] args){
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

