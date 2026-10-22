/**
 * Ambulance.java
 * A simple class that stores one ambulance: its id, its location (x, y in km)
 * and whether it is available or busy.
 */
public class Ambulance {
    int id;
    double x;
    double y;
    boolean available;   // true = free to dispatch, false = busy

    public Ambulance(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.available = true;
    }

    /** Straight-line distance from this ambulance to the point (px, py). */
    public double distanceTo(double px, double py) {
        double dx = x - px;
        double dy = y - py;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
