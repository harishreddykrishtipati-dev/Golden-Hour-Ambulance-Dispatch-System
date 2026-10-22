import java.util.Random;

/**
 * Tests.java - proves the grid gives EXACTLY the same answers as brute force.
 * Run:  java Tests
 */
public class Tests {

    static int checks = 0;

    static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new RuntimeException("TEST FAILED: " + message);
        }
    }

    // ------------------------------------------------------------
    static void testBasic() {
        SpatialHashGrid g = new SpatialHashGrid(2);
        Ambulance a1 = new Ambulance(1, 1, 1);
        Ambulance a2 = new Ambulance(2, 9, 9);
        g.insert(a1);
        g.insert(a2);
        check(g.nearest(0, 0).id == 1, "nearest should be 1");
        g.remove(a1);
        check(g.nearest(0, 0).id == 2, "after removing 1, nearest should be 2");
        g.remove(a2);
        check(g.nearest(0, 0) == null, "empty grid should return null");
        System.out.println("basic tests ok");
    }

    static void testNegativeAndEdge() {
        SpatialHashGrid g = new SpatialHashGrid(2);
        Ambulance a1 = new Ambulance(1, -3.5, -0.1);
        Ambulance a2 = new Ambulance(2, 0.0, 0.0);   // exactly on a cell corner
        g.insert(a1);
        g.insert(a2);
        check(g.nearest(-3, 0).id == 1, "negative coordinates");
        check(g.nearest(0.1, 0.1).id == 2, "point on cell corner");
        System.out.println("negative / edge-case tests ok");
    }

    // ------------------------------------------------------------
    /** Random fleets, random moves/removals, random queries - grid vs brute force. */
    static void testRandomAgainstBruteForce(int trials) {
        Random rnd = new Random(7);
        double[] cellSizes = {0.5, 1, 2, 5, 10};
        int queries = 0;

        for (int t = 0; t < trials; t++) {
            double cs = cellSizes[rnd.nextInt(cellSizes.length)];
            SpatialHashGrid grid = new SpatialHashGrid(cs);
            BruteForceFinder brute = new BruteForceFinder();

            int n = 1 + rnd.nextInt(80);
            Ambulance[] forGrid = new Ambulance[n];
            Ambulance[] forBrute = new Ambulance[n];
            boolean[] present = new boolean[n];

            for (int i = 0; i < n; i++) {
                double x = rnd.nextDouble() * 50;
                double y = rnd.nextDouble() * 50;
                forGrid[i] = new Ambulance(i, x, y);
                forBrute[i] = new Ambulance(i, x, y);   // separate copy
                grid.insert(forGrid[i]);
                brute.insert(forBrute[i]);
                present[i] = true;
            }

            // random moves and removals, like a live fleet
            for (int step = 0; step < 30; step++) {
                int i = rnd.nextInt(n);
                if (!present[i]) continue;
                if (rnd.nextDouble() < 0.3) {
                    grid.remove(forGrid[i]);
                    brute.remove(forBrute[i]);
                    present[i] = false;
                } else {
                    double x = rnd.nextDouble() * 50;
                    double y = rnd.nextDouble() * 50;
                    grid.move(forGrid[i], x, y);
                    forBrute[i].x = x;
                    forBrute[i].y = y;
                }
            }

            // random accidents, some outside the city
            for (int q = 0; q < 20; q++) {
                double qx = rnd.nextDouble() * 60 - 5;
                double qy = rnd.nextDouble() * 60 - 5;
                Ambulance fromGrid = grid.nearest(qx, qy);
                Ambulance fromBrute = brute.nearest(qx, qy);
                queries++;

                if (fromBrute == null) {
                    check(fromGrid == null, "both should be empty (trial " + t + ")");
                } else {
                    check(fromGrid != null, "grid found nothing (trial " + t + ")");
                    double dg = fromGrid.distanceTo(qx, qy);
                    double db = fromBrute.distanceTo(qx, qy);
                    check(Math.abs(dg - db) < 1e-9,
                          "distance mismatch in trial " + t + ": grid=" + dg + " brute=" + db);
                }
            }
        }
        System.out.println(trials + " random trials (" + queries + " queries) match brute force");
    }

    public static void main(String[] args) {
        testBasic();
        testNegativeAndEdge();
        testRandomAgainstBruteForce(300);
        System.out.println("ALL TESTS PASSED (" + checks + " checks)");
    }
}
