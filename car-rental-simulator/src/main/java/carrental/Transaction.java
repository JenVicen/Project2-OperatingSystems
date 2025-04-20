package carrental;

import java.io.Serializable;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String licensePlate;
    private int kilometers;
    private boolean discountApplied;
    private double charge;

    public Transaction(String licensePlate, int kilometers, boolean discountApplied, double charge) {
        this.licensePlate = licensePlate;
        this.kilometers = kilometers;
        this.discountApplied = discountApplied;
        this.charge = charge;
    }

    // Getter for licensePlate
    public String getLicensePlate() {
        return licensePlate;
    }

    // Getter for kilometers
    public int getKilometers() {
        return kilometers;
    }

    public boolean isDiscountApplied() {
        return discountApplied;
    }

    public double getCharge() {
        return charge;
    }

    @Override
    public String toString() {
        return "Vehicle " + licensePlate + " | Km: " + kilometers 
            + " | Discount: " + (discountApplied ? "10%" : "0%") 
            + " | Charge: $" + charge;
    }
}
