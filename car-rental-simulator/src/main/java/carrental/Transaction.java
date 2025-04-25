/*
By Jennifer Vicentes

Purpose: This class represents a transaction in the car rental system. It contains information about the vehicle's license plate, kilometers driven, whether a discount was applied, and the total charge for the rental.
It also provides methods to get this information and to display the transaction details.
This class implements Serializable to allow saving and loading of transaction objects.
This is important for saving the state of the object to a file and loading it back.

All the comments I wrote were put for me to keep track while developing, they are not AI generated. 
*/
package carrental;

import java.io.Serializable;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String licensePlate;
    private int kilometers;
    private boolean discountApplied;
    private double charge;

    // Constructor to initialize the transaction with a license plate, kilometers, discount status, and charge
    public Transaction(String licensePlate, int kilometers, boolean discountApplied, double charge) {
        this.licensePlate = licensePlate;
        this.kilometers = kilometers;
        this.discountApplied = discountApplied;
        this.charge = charge;
    }

    // Get the license plate of the vehicle
    public String getLicensePlate() {
        return licensePlate;
    }

    // Get the kilometers driven by the vehicle
    public int getKilometers() {
        return kilometers;
    }

    // Check if a discount was applied to the transaction
    // This method returns true if a discount was applied, false otherwise
    public boolean isDiscountApplied() {
        return discountApplied;
    }

    // Get the total charge for the transaction
    public double getCharge() {
        return charge;
    }

    @Override
    // The string representation includes the license plate, kilometers driven, discount status, and total charge
    // The discount status is displayed as "10%" if a discount was applied, and "0%" otherwise
    public String toString() {
        return "Vehicle " + licensePlate + " | Km: " + kilometers 
            + " | Discount: " + (discountApplied ? "10%" : "0%") 
            + " | Charge: $" + charge;
    }
}
