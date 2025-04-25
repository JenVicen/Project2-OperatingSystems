/*
By Jennifer Vicentes
Purpose: This class represents a vehicle in the car rental system. It contains information about the vehicle's license plate, type, and kilometers driven.
It also provides methods to get this information and to add kilometers driven to the vehicle.

All the comments I wrote were put for me to keep track while developing, they are not AI generated. 
*/

package carrental;
import java.io.Serializable;

// Implements serializable to allow saving and loading of vehicle objects
public class Vehicle implements Serializable {
    // Serial version UID for serialization, this means that the class can be serialized and deserialized
    // This is important for saving the state of the object to a file and loading it back
    // The serialVersionUID is a unique identifier for the class version
    private static final long serialVersionUID = 1L;
    private String licensePlate;
    private String type; // "SEDAN", "SUV", or "VAN"
    private int kilometers;

    // Constructor to initialize the vehicle with a license plate, type, and kilometers
    public Vehicle(String licensePlate, String type, int kilometers) {
        this.licensePlate = licensePlate;
        this.type = type;
        this.kilometers = kilometers;
    }

    // Get the license plate of the vehicle
    public String getLicensePlate() {
        return licensePlate;
    }

    // Get the type of the vehicle
    // This can be "SEDAN", "SUV", or "VAN"
    public String getType() {
        return type;
    }

    // Get the kilometers driven by the vehicle
    public int getKilometers() {
        return kilometers;
    }

    // Add kilometers to the vehicle
    // This method is used when the vehicle is returned to the rental shop
    // It updates the kilometers driven by the vehicle
    public void addKilometers(int km) {
        this.kilometers += km;
    }

    @Override
    // Override the toString method to provide a string representation of the vehicle
    // The string representation includes the license plate, type, and kilometers driven
    public String toString() {
        return licensePlate + " (" + type + "), Km: " + kilometers;
    }
}
