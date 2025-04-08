package carrental;
import java.io.Serializable;

public class Vehicle implements Serializable {
    private static final long serialVersionUID = 1L;
    private String licensePlate;
    private String type; // "SEDAN", "SUV", or "VAN"
    private int kilometers;

    public Vehicle(String licensePlate, String type, int kilometers) {
        this.licensePlate = licensePlate;
        this.type = type;
        this.kilometers = kilometers;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getType() {
        return type;
    }

    public int getKilometers() {
        return kilometers;
    }

    public void addKilometers(int km) {
        this.kilometers += km;
    }

    @Override
    public String toString() {
        return licensePlate + " (" + type + "), Km: " + kilometers;
    }
}
