import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * DispatchSystem.java - the application layer.
 *
 * It manages the fleet (who is available / busy) and uses the
 * SpatialHashGrid to find the nearest AVAILABLE ambulance quickly.
 *
 * Key idea: only AVAILABLE ambulances live inside the grid.
 * When one is dispatched it is removed from the grid, so a search can never
 * pick a busy ambulance. When it finishes its job it is put back.
 */
public class DispatchSystem {

    static final double SPEED_KMPH = 40.0;      // assumed average speed in city traffic
    static final double TARGET_MINUTES = 10.0;  // response-time target for critical cases

    double cityKm;
    SpatialHashGrid grid;
    HashMap<Integer, Ambulance> fleet = new HashMap<>();
    ArrayList<DispatchResult> log = new ArrayList<>();
    Random random;

    public DispatchSystem(double cityKm, double cellSize, long seed) {
        this.cityKm = cityKm;
        this.grid = new SpatialHashGrid(cellSize);
        this.random = new Random(seed);
    }

    // ---------- fleet management ----------
    public void addAmbulance(int id, double x, double y) {
        Ambulance a = new Ambulance(id, x, y);
        fleet.put(id, a);
        grid.insert(a);
    }

    /** GPS ping. Only available ambulances are in the grid, so only they are moved there. */
    public void updateLocation(int id, double x, double y) {
        Ambulance a = fleet.get(id);
        if (a.available) {
            grid.move(a, x, y);
        } else {
            a.x = x;
            a.y = y;
        }
    }

    // ---------- the main use case ----------
    /** Send the nearest available ambulance to an accident at (ax, ay). */
    public DispatchResult dispatch(double ax, double ay) {
        Ambulance a = grid.nearest(ax, ay);
        if (a == null) {
            return null;                    // no ambulance is free
        }
        double dist = a.distanceTo(ax, ay);
        grid.remove(a);                     // busy ambulances leave the grid
        a.available = false;

        double eta = dist / SPEED_KMPH * 60.0;
        DispatchResult r = new DispatchResult(ax, ay, a, dist, eta, eta <= TARGET_MINUTES);
        log.add(r);
        return r;
    }

    /** Ambulance finished its job (for example at a hospital located at x, y). */
    public void completeJob(int id, double x, double y) {
        Ambulance a = fleet.get(id);
        a.x = x;
        a.y = y;
        a.available = true;
        grid.insert(a);
    }

    // ---------- helpers ----------
    public int availableCount() {
        return grid.count;
    }

    public double randomCoordinate() {
        return random.nextDouble() * cityKm;
    }
}
