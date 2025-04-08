package carrental;

import java.io.*;
import java.util.*;

public class LotManager {
    private static final String REGISTRY_FILE = "plates_registry.txt";

    public static void main(String[] args) {
        Map<String, String> flags = parseArgs(args);
        String lotName = flags.get("--lot-name");
        if(lotName == null || lotName.isEmpty()){
            System.err.println("Error: --lot-name must be provided.");
            System.exit(1);
        }
        String lotFile = lotName + ".txt";
        List<Vehicle> lotVehicles = readLotFile(lotFile);
        
        // Process add flags
        if(flags.containsKey("--add-sedan")){
            int n = Integer.parseInt(flags.get("--add-sedan"));
            for(int i = 0; i < n; i++){
                lotVehicles.add(new Vehicle(generateUniquePlate(), "SEDAN", 0));
            }
        }
        if(flags.containsKey("--add-suv")){
            int n = Integer.parseInt(flags.get("--add-suv"));
            for(int i = 0; i < n; i++){
                lotVehicles.add(new Vehicle(generateUniquePlate(), "SUV", 0));
            }
        }
        if(flags.containsKey("--add-van")){
            int n = Integer.parseInt(flags.get("--add-van"));
            for(int i = 0; i < n; i++){
                lotVehicles.add(new Vehicle(generateUniquePlate(), "VAN", 0));
            }
        }
        // Process remove flag
        if(flags.containsKey("--remove-vehicle")){
            String plateToRemove = flags.get("--remove-vehicle");
            boolean removed = lotVehicles.removeIf(v -> v.getLicensePlate().equals(plateToRemove));
            if(!removed){
                System.out.println("Vehicle with license " + plateToRemove + " not found in lot " + lotName);
            }
        }
        
        writeLotFile(lotFile, lotVehicles);
        System.out.println("Lot " + lotName + " updated successfully. Total vehicles: " + lotVehicles.size());
    }
    
    // Parse command line arguments into a map.
    private static Map<String, String> parseArgs(String[] args){
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
    
    // Reads a lot file where each line is: licensePlate,type,kilometers
    private static List<Vehicle> readLotFile(String fileName){
        List<Vehicle> vehicles = new ArrayList<>();
        File file = new File(fileName);
        if(!file.exists()){
            return vehicles;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null){
                String[] parts = line.split(",");
                if(parts.length >= 3){
                    String plate = parts[0].trim();
                    String type = parts[1].trim();
                    int km = Integer.parseInt(parts[2].trim());
                    vehicles.add(new Vehicle(plate, type, km));
                }
            }
        } catch(IOException e){
            System.err.println("Error reading lot file: " + e.getMessage());
        }
        return vehicles;
    }
    
    // Writes the list of vehicles to the lot file.
    private static void writeLotFile(String fileName, List<Vehicle> vehicles){
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            for(Vehicle v : vehicles){
                pw.println(v.getLicensePlate() + "," + v.getType() + "," + v.getKilometers());
            }
        } catch(IOException e){
            System.err.println("Error writing lot file: " + e.getMessage());
        }
    }
    
    // Generates a unique license plate following Costa Rican rules (3 letters-dash-3 digits)
    private static String generateUniquePlate(){
        Set<String> registry = loadRegistry();
        Random random = new Random();
        String plate;
        do {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < 3; i++){
                char letter = (char) ('A' + random.nextInt(26));
                sb.append(letter);
            }
            sb.append("-");
            for(int i = 0; i < 3; i++){
                sb.append(random.nextInt(10));
            }
            plate = sb.toString();
        } while(registry.contains(plate));
        registry.add(plate);
        saveRegistry(registry);
        return plate;
    }
    
    // Loads the global registry of license plates.
    private static Set<String> loadRegistry(){
        Set<String> registry = new HashSet<>();
        File file = new File(REGISTRY_FILE);
        if(!file.exists()){
            return registry;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null){
                registry.add(line.trim());
            }
        } catch(IOException e){
            System.err.println("Error reading registry file: " + e.getMessage());
        }
        return registry;
    }
    
    // Saves the registry of license plates.
    private static void saveRegistry(Set<String> registry){
        try (PrintWriter pw = new PrintWriter(new FileWriter(REGISTRY_FILE))) {
            for(String plate : registry){
                pw.println(plate);
            }
        } catch(IOException e){
            System.err.println("Error writing registry file: " + e.getMessage());
        }
    }
}

