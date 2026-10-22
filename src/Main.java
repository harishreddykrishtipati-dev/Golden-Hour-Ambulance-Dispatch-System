import java.util.Random;
import java.util.Scanner;

/**
 * Main.java - live demo of the Golden-Hour Ambulance Dispatch System.
 *
 *   java Main            -> scripted demo (best for presenting)
 *   java Main manual     -> you type accident locations yourself
 */
public class Main {

    static final double CITY_KM = 50.0;
    static final int FLEET = 40;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("manual")) {
            manualMode();
        } else {
            simulation();
            asciiMapDemo();
        }
    }

    // ------------------------------------------------------------
    static void banner(String text) {
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("  " + text);
        System.out.println("==============================================================");
    }

    static void show(DispatchResult r, String label) {
        if (r == null) {
            System.out.println("  !! No ambulance available");
            return;
        }
        String tag = r.withinTarget ? "WITHIN TARGET" : "LATE (> target)";
        System.out.printf("  %sAccident at (%5.1f, %5.1f) -> Ambulance #%-3d | %5.2f km | ETA %4.1f min | %s%n",
                label, r.accidentX, r.accidentY, r.ambulance.id, r.distanceKm, r.etaMinutes, tag);
    }

    // ------------------------------------------------------------
    static void simulation() {
        DispatchSystem sys = new DispatchSystem(CITY_KM, 2.0, 5);

        banner("STEP 1: Register " + FLEET + " ambulances in a 50 x 50 km city");
        for (int i = 0; i < FLEET; i++) {
            sys.addAmbulance(i, sys.randomCoordinate(), sys.randomCoordinate());
        }
        System.out.println("  Ambulances in grid : " + sys.grid.count);
        System.out.println("  Non-empty cells    : " + sys.grid.cells.size()
                + "  (cell size " + sys.grid.cellSize + " km)");

        banner("STEP 2: Accidents arrive - dispatch the nearest available ambulance");
        for (int n = 1; n <= 5; n++) {
            DispatchResult r = sys.dispatch(sys.randomCoordinate(), sys.randomCoordinate());
            show(r, "#" + n + " ");
            System.out.println("      (grid checked " + sys.grid.lastChecked + " of "
                    + (sys.availableCount() + 1) + " free ambulances)");
        }
        System.out.println("  Still available: " + sys.availableCount() + "/" + FLEET);

        banner("STEP 3: Ambulances keep moving (GPS pings), then a new accident");
        Random wiggle = new Random(99);
        for (int id = 0; id < FLEET; id++) {
            Ambulance a = sys.fleet.get(id);
            if (a.available) {
                double nx = Math.min(CITY_KM, Math.max(0, a.x + (wiggle.nextDouble() * 2 - 1)));
                double ny = Math.min(CITY_KM, Math.max(0, a.y + (wiggle.nextDouble() * 2 - 1)));
                sys.updateLocation(id, nx, ny);
            }
        }
        show(sys.dispatch(sys.randomCoordinate(), sys.randomCoordinate()), "#6 ");

        banner("STEP 4: A busy ambulance finishes its job and rejoins the grid");
        int busyId = sys.log.get(0).ambulance.id;
        double hx = sys.randomCoordinate();
        double hy = sys.randomCoordinate();
        sys.completeJob(busyId, hx, hy);
        System.out.printf("  Ambulance #%d is AVAILABLE again at (%.1f, %.1f)%n", busyId, hx, hy);
        System.out.println("  Available now: " + sys.availableCount() + "/" + FLEET);

        banner("STEP 5: Rush hour - up to 100 accidents in a row");
        DispatchSystem rush = new DispatchSystem(CITY_KM, 2.0, 5);
        for (int i = 0; i < FLEET; i++) {
            rush.addAmbulance(i, rush.randomCoordinate(), rush.randomCoordinate());
        }
        int served = 0;
        for (int i = 0; i < 100; i++) {
            DispatchResult r = rush.dispatch(rush.randomCoordinate(), rush.randomCoordinate());
            if (r == null) {
                break;                      // fleet exhausted
            }
            served++;
        }
        int ok = 0;
        double total = 0;
        for (int i = 0; i < rush.log.size(); i++) {
            DispatchResult r = rush.log.get(i);
            if (r.withinTarget) ok++;
            total += r.etaMinutes;
        }
        System.out.println("  Served " + served + " accidents, then the fleet was exhausted");
        System.out.println("  " + ok + "/" + served + " reached within the "
                + (int) DispatchSystem.TARGET_MINUTES + "-minute target");
        System.out.printf("  Average ETA: %.1f min%n", total / served);
    }

    // ------------------------------------------------------------
    /** Draws the city as text so you can SEE which cells the search visited. */
    static void asciiMapDemo() {
        banner("STEP 6: Map of one search (30 x 30 km city, 3 km cells, 60 ambulances)");
        SpatialHashGrid grid = new SpatialHashGrid(3.0);
        Random rnd = new Random(11);
        for (int i = 0; i < 60; i++) {
            grid.insert(new Ambulance(i, rnd.nextDouble() * 30, rnd.nextDouble() * 30));
        }

        double ax = 18.2, ay = 12.4;
        Ambulance best = grid.nearest(ax, ay);
        int cx = grid.colOf(ax);
        int cy = grid.rowOf(ay);

        for (int row = 9; row >= 0; row--) {
            System.out.print("  ");
            for (int col = 0; col < 10; col++) {
                boolean searched = Math.abs(col - cx) <= grid.lastRings
                                && Math.abs(row - cy) <= grid.lastRings;
                String symbol;
                if (col == cx && row == cy) {
                    symbol = "X";
                } else {
                    java.util.ArrayList<Ambulance> list = grid.cells.get(grid.keyOf(col, row));
                    int c = (list == null) ? 0 : list.size();
                    symbol = (c == 0) ? "." : String.valueOf(c);
                }
                System.out.print(searched ? "[" + symbol + "]" : " " + symbol + " ");
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("  X = accident cell, digits = ambulances in that cell, [ ] = cells searched");
        System.out.printf("  Nearest: ambulance #%d at %.2f km%n", best.id, best.distanceTo(ax, ay));
        System.out.println("  Grid checked " + grid.lastChecked + " of " + grid.count + " ambulances");
    }

    // ------------------------------------------------------------
    static void manualMode() {
        DispatchSystem sys = new DispatchSystem(CITY_KM, 2.0, System.nanoTime());
        for (int i = 0; i < FLEET; i++) {
            sys.addAmbulance(i, sys.randomCoordinate(), sys.randomCoordinate());
        }
        System.out.println(FLEET + " ambulances placed in a 50 x 50 km city.");
        System.out.println("Type the accident location as:  x y   (0 to 50)   or  q  to quit");

        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().trim();
            if (line.equals("q")) break;
            String[] parts = line.split("\\s+");
            if (parts.length != 2) {
                System.out.println("  please type two numbers, e.g.  12 30");
                continue;
            }
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                show(sys.dispatch(x, y), "");
                System.out.println("  available: " + sys.availableCount() + "/" + FLEET);
            } catch (NumberFormatException e) {
                System.out.println("  please type two numbers, e.g.  12 30");
            }
        }
    }
}
