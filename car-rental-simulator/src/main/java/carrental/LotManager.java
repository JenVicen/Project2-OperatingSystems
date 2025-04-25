/*
By Jennifer Vicentes
Purpose: This class manages a car rental lot, allowing the addition and removal of vehicles.
It handles command line arguments to specify the lot name and the number of vehicles to add or remove.
It also generates unique license plates for the vehicles, ensuring they follow Costa Rican regulations.
It reads and writes vehicle data to a file, maintaining a registry of license plates to avoid duplicates.

All the comments I wrote were put for me to keep track while developing, they are not AI generated. 
*/

package carrental;

import java.io.*;
import java.util.*;

public class LotManager {
    // This keeps track of the license plates in the registry and ensures that no two vehicles have the same plate.
    private static final String REGISTRY_FILE = "plates_registry.txt";

    public static void main(String[] args) {
        // Parse command line arguments into a map. The map allows for easy access to the flags and their values.
        Map<String, String> flags = parseArgs(args);
        // Check if the --lot-name flag is provided. If not, print an error message and exit.
        String lotName = flags.get("--lot-name");
        if(lotName == null || lotName.isEmpty()){
            System.err.println("Error: --lot-name must be provided.");
            System.exit(1);
        }
        // Read the lot file. Each lot is represented by a text file where each line contains vehicle information.
        String lotFile = lotName + ".txt";
        List<Vehicle> lotVehicles = readLotFile(lotFile);
        
        // Process add flags, which specify the number of vehicles to add to the lot.
        if(flags.containsKey("--add-sedan")){
            // Get the number of sedans to add from the command line arguments.
            int n = Integer.parseInt(flags.get("--add-sedan"));
            for(int i = 0; i < n; i++){
                lotVehicles.add(new Vehicle(generateUniquePlate(), "SEDAN", 0));
            }
        }
        // Process add flags for SUVs and vans in a similar manner.
        if(flags.containsKey("--add-suv")){
            int n = Integer.parseInt(flags.get("--add-suv"));
            for(int i = 0; i < n; i++){
                lotVehicles.add(new Vehicle(generateUniquePlate(), "SUV", 0));
            }
        }
        // Process add flags for vans.
        if(flags.containsKey("--add-van")){
            int n = Integer.parseInt(flags.get("--add-van"));
            for(int i = 0; i < n; i++){
                lotVehicles.add(new Vehicle(generateUniquePlate(), "VAN", 0));
            }
        }
        // Process remove flag, which specifies a vehicle to remove from the lot.
        if(flags.containsKey("--remove-vehicle")){
            String plateToRemove = flags.get("--remove-vehicle");
            // Check if the vehicle with the specified license plate exists in the lot.
            // If it does, remove it from the lot. If not, print an error message.
            // The removeIf method is used to remove the vehicle based on its license plate.
            // The equalsIgnoreCase method is used to ensure that the comparison is case-insensitive.
            // This is important because license plates can be entered in different cases (e.g., "ABC-123" vs "abc-123").
            boolean removed = lotVehicles.removeIf(v -> v.getLicensePlate().equalsIgnoreCase(plateToRemove));
            if(!removed){
                System.out.println("Vehicle with license " + plateToRemove 
                        + " not found in lot " + lotName 
                        + " (possibly assigned to a shop or is in use).");
            } else {
                // If the vehicle was successfully removed, print a success message.
                System.out.println("Vehicle with license " + plateToRemove + " successfully removed from lot " + lotName + ".");
            }
        }
        
        // Write the updated list of vehicles back to the lot file.
        // This ensures that the lot file is always up-to-date with the current state of the vehicles in the lot.
        writeLotFile(lotFile, lotVehicles);
        // Print a success message indicating that the lot has been updated successfully.
        System.out.println("Lot " + lotName + " updated successfully. Total vehicles: " + lotVehicles.size());
    }
    
    // Parse command line arguments into a map.
    private static Map<String, String> parseArgs(String[] args){
        Map<String, String> flags = new HashMap<>();
        // Iterate through the command line arguments.
        // Each argument is checked to see if it starts with "--", indicating that it's a flag.
        for(String arg: args){
            if(arg.startsWith("--")){
                // If the argument contains an '=', it is split into a key and value.
                int eq = arg.indexOf('=');
                // If the '=' is found, the key and value are extracted.
                // If not, the argument is treated as a flag with an empty value.
                if(eq > 0){
                    String key = arg.substring(0, eq);
                    String value = arg.substring(eq + 1);
                    flags.put(key, value);
                } else {
                    flags.put(arg, "");
                }
            }
        }
        // It returns the map of flags and their values.
        return flags;
    }
    
    // Reads a lot file where each line is: licensePlate,type,kilometers
    private static List<Vehicle> readLotFile(String fileName){
        // This method reads the lot file and returns a list of vehicles.
        List<Vehicle> vehicles = new ArrayList<>();
        // The file is read line by line, and each line is split into parts based on the comma delimiter.
        File file = new File(fileName);
        if(!file.exists()){
            // If the file does not exist, it returns an empty list of vehicles.
            return vehicles;
        }
        // The BufferedReader is used to read the file efficiently. BufferedReader reads text from a character-input stream, buffering characters to provide efficient reading of characters, arrays, and lines.
        // Each line is split into parts: license plate, type, and kilometers.
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            // Read each line of the file.
            String line;
            while((line = br.readLine()) != null){
                String[] parts = line.split(",");
                // Check if the line has at least 3 parts (license plate, type, kilometers).
                if(parts.length >= 3){
                    String plate = parts[0].trim();
                    String type = parts[1].trim();
                    int km = Integer.parseInt(parts[2].trim());
                    vehicles.add(new Vehicle(plate, type, km));
                } else{
                    // If the line does not have enough parts, it is ignored.
                    // This is important to ensure that the program does not crash due to unexpected file format.
                    System.err.println("Invalid line in lot file: " + line);
                }
                // If the line does not have enough parts, it is ignored.
            }
        } catch(IOException e){
            System.err.println("Error reading lot file: " + e.getMessage());
        }
        return vehicles;
    }
    
    // Writes the list of vehicles to the lot file.
    private static void writeLotFile(String fileName, List<Vehicle> vehicles){
        // This method writes the list of vehicles to the lot file. PrintWriter is used to write formatted text to a file.
        // It provides methods to write strings, characters, and other data types.
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
        // This method generates a unique license plate. It uses a random number generator to create a plate in the format "XXX-000".
        Random random = new Random();
        // The license plate is generated in a loop until a unique one is found.
        String plate;
        do {
            // Generate 3 random letters (A-Z).
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < 3; i++){
                // Generate a random letter from A to Z.
                // The random number generator is used to select a letter from the alphabet.
                char letter = (char) ('A' + random.nextInt(26));
                // Append the letter to the StringBuilder.
                sb.append(letter);
            }
            // Generate 3 random digits (0-9).
            sb.append("-");
            for(int i = 0; i < 3; i++){
                sb.append(random.nextInt(10));
            }
            // Convert the StringBuilder to a string.
            plate = sb.toString();
            // Check if the generated plate is already in the registry.
        } while(registry.contains(plate));
        // If the plate is unique, it is added to the registry and saved to the file.
        registry.add(plate);
        // The registry is saved to the file to ensure that the unique plate is not generated again in the future.
        saveRegistry(registry);
        return plate;
    }
    
    // Loads the global registry of license plates.
    private static Set<String> loadRegistry(){
        // This method loads the registry of license plates from a file. The registry is stored in a text file where each line contains a unique license plate.
        Set<String> registry = new HashSet<>();
        // The registry file is read line by line, and each line is added to the set of license plates.
        File file = new File(REGISTRY_FILE);
        // If the registry file does not exist, it returns an empty set of license plates.
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
        // This method saves the registry of license plates to a file. The registry is stored in a text file where each line contains a unique license plate.
        try (PrintWriter pw = new PrintWriter(new FileWriter(REGISTRY_FILE))) {
            // The registry is written to the file line by line.
            for(String plate : registry){
                // Each license plate is written to the file.
                pw.println(plate);
            }
        } catch(IOException e){
            System.err.println("Error writing registry file: " + e.getMessage());
        }
    }
}

