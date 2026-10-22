import java.util.ArrayList;
import java.util.HashMap;

/**
 * SpatialHashGrid.java  -  the data structure of our case study
 * -------------------------------------------------------------
 * The city is a flat square (coordinates in km). We cut it into small square
 * cells of side "cellSize". Every cell is stored in a HashMap:
 *
 *      key   = a number that identifies the cell (its column and row)
 *      value = a list of the ambulances that are inside that cell
 *
 * Only cells that contain at least one ambulance are stored.
 *
 * Cost (n = number of ambulances):
 *      insert / remove / move : about O(1)
 *      nearest                : about O(1) with a good cell size (brute force: O(n))
 */
public class SpatialHashGrid {

    double cellSize;
    HashMap<Long, ArrayList<Ambulance>> cells = new HashMap<>();

    // Bounding box of all points ever inserted. Used only to decide how far
    // the ring search is allowed to go, so that it always finishes.
    double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
    double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
    int count = 0;   // how many ambulances are currently in the grid

    // Statistics about the most recent nearest() call (used by the demo to
    // show how little of the city was actually searched).
    int lastRings = 0;
    int lastChecked = 0;

    public SpatialHashGrid(double cellSize) {
        this.cellSize = cellSize;
    }

    // ---------------------------------------------------------------
    // Step 1: turn a location into a cell
    // ---------------------------------------------------------------
    int colOf(double x) {
        return (int) Math.floor(x / cellSize);
    }

    int rowOf(double y) {
        return (int) Math.floor(y / cellSize);
    }

    /**
     * Combine column and row into ONE number so it can be a HashMap key.
     * Example: column 2, row 4  ->  2 * 1000003 + 4 = 2000010.
     * (1000003 is just a big number; it keeps different cells from clashing.)
     */
    long keyOf(int col, int row) {
        return (long) col * 1000003L + row;
    }

    // ---------------------------------------------------------------
    // Step 2: insert / remove / move
    // ---------------------------------------------------------------
    public void insert(Ambulance a) {
        long key = keyOf(colOf(a.x), rowOf(a.y));
        ArrayList<Ambulance> list = cells.get(key);
        if (list == null) {                 // first ambulance in this cell
            list = new ArrayList<>();
            cells.put(key, list);
        }
        list.add(a);
        count++;
        updateBounds(a.x, a.y);
    }

    public void remove(Ambulance a) {
        long key = keyOf(colOf(a.x), rowOf(a.y));
        ArrayList<Ambulance> list = cells.get(key);
        list.remove(a);
        if (list.isEmpty()) {               // delete empty cells to save memory
            cells.remove(key);
        }
        count--;
    }

    /**
     * Ambulance sends a new GPS location.
     * If it is still in the same cell we only change its coordinates.
     * If it crossed into another cell we take it out of the old cell and
     * put it in the new one.
     */
    public void move(Ambulance a, double newX, double newY) {
        long oldKey = keyOf(colOf(a.x), rowOf(a.y));
        long newKey = keyOf(colOf(newX), rowOf(newY));

        if (oldKey != newKey) {
            ArrayList<Ambulance> oldList = cells.get(oldKey);
            oldList.remove(a);
            if (oldList.isEmpty()) {
                cells.remove(oldKey);
            }
            ArrayList<Ambulance> newList = cells.get(newKey);
            if (newList == null) {
                newList = new ArrayList<>();
                cells.put(newKey, newList);
            }
            newList.add(a);
        }
        a.x = newX;
        a.y = newY;
        updateBounds(newX, newY);
    }

    void updateBounds(double x, double y) {
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
    }

    // ---------------------------------------------------------------
    // Step 3: the main query - find the nearest ambulance
    // ---------------------------------------------------------------
    /**
     * Search the accident's own cell first (ring 0), then the ring of cells
     * around it (ring 1), then ring 2, and so on.
     *
     * STOP RULE: after finishing ring r, any ambulance we have not looked at
     * is in ring r+1 or further, so it is at least (r * cellSize) away.
     * If the best ambulance found so far is already that close, nothing
     * better can exist, so we stop. This keeps the answer exactly correct.
     *
     * Returns null if the grid is empty.
     */
    public Ambulance nearest(double x, double y) {
        if (count == 0) {
            return null;
        }

        int cx = colOf(x);
        int cy = rowOf(y);

        // farthest any ambulance could possibly be -> limit on number of rings
        double far = Math.max(Math.max(Math.abs(maxX - x), Math.abs(x - minX)),
                              Math.max(Math.abs(maxY - y), Math.abs(y - minY)));
        int maxRing = (int) (far / cellSize) + 2;

        Ambulance best = null;
        double bestDist = Double.MAX_VALUE;
        lastChecked = 0;

        for (int ring = 0; ring <= maxRing; ring++) {
            lastRings = ring;

            // visit every cell on the square ring that is "ring" steps away
            for (int col = cx - ring; col <= cx + ring; col++) {
                for (int row = cy - ring; row <= cy + ring; row++) {

                    // inside a ring we only want the border cells
                    boolean onBorder = (col == cx - ring || col == cx + ring
                                     || row == cy - ring || row == cy + ring);
                    if (!onBorder) {
                        continue;
                    }

                    ArrayList<Ambulance> list = cells.get(keyOf(col, row));
                    if (list == null) {
                        continue;           // empty cell
                    }
                    for (int i = 0; i < list.size(); i++) {
                        Ambulance a = list.get(i);
                        lastChecked++;
                        double d = a.distanceTo(x, y);
                        if (d < bestDist) {
                            bestDist = d;
                            best = a;
                        }
                    }
                }
            }

            // the stop rule
            if (best != null && bestDist <= ring * cellSize) {
                break;
            }
        }
        return best;
    }

}
