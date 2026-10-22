import java.util.ArrayList;

/**
 * BruteForceFinder.java
 * The simple, obvious solution: keep all ambulances in one list and check
 * EVERY one of them for each accident. O(n) per query.
 * We use it to (1) test that the grid is correct and (2) compare speed.
 */
public class BruteForceFinder {

    ArrayList<Ambulance> all = new ArrayList<>();

    public void insert(Ambulance a) {
        all.add(a);
    }

    public void remove(Ambulance a) {
        all.remove(a);
    }

    public Ambulance nearest(double x, double y) {
        Ambulance best = null;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < all.size(); i++) {
            Ambulance a = all.get(i);
            double d = a.distanceTo(x, y);
            if (d < bestDist) {
                bestDist = d;
                best = a;
            }
        }
        return best;
    }
}
