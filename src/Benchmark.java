import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * Benchmark.java - Spatial Hash Grid vs Brute Force.
 * Run:  java Benchmark      (takes about a minute)
 * Prints two tables and saves them to results.csv so you can make charts in Excel.
 */
public class Benchmark {

    static final double CITY = 50.0;
    static final int QUERIES = 3000;

    static double checksum = 0;   // makes sure Java really performs every query

    public static void main(String[] args) throws IOException {
        Random rnd = new Random(1);

        // the same accident locations are used for every test
        double[] qx = new double[QUERIES];
        double[] qy = new double[QUERIES];
        for (int i = 0; i < QUERIES; i++) {
            qx[i] = rnd.nextDouble() * CITY;
            qy[i] = rnd.nextDouble() * CITY;
        }

        FileWriter csv = new FileWriter("results.csv");
        csv.write("experiment,n_or_cellsize,grid_ms,brute_ms\n");

        // ---------------- Experiment 1: growing fleet ----------------
        int[] sizes = {100, 500, 1000, 5000, 10000, 20000, 50000};
        System.out.println("Experiment 1: query time as the fleet grows");
        System.out.println("(cell size = city width / sqrt(n), so about 1 ambulance per cell)");
        System.out.println("Ambulances | Grid ms/query | Brute ms/query | Speed-up");
        System.out.println("-----------+---------------+----------------+---------");

        for (int s = 0; s < sizes.length; s++) {
            int n = sizes[s];
            double cellSize = CITY / Math.sqrt(n);
            SpatialHashGrid grid = new SpatialHashGrid(cellSize);
            BruteForceFinder brute = new BruteForceFinder();
            for (int i = 0; i < n; i++) {
                Ambulance a = new Ambulance(i, rnd.nextDouble() * CITY, rnd.nextDouble() * CITY);
                grid.insert(a);
                brute.insert(a);
            }

            // warm-up: let Java's JIT compiler optimise before we measure
            timeGrid(grid, qx, qy);
            timeBrute(brute, qx, qy);

            double g = timeGrid(grid, qx, qy);
            double b = timeBrute(brute, qx, qy);
            System.out.printf("%10d | %13.4f | %14.4f | %7.1fx%n", n, g, b, b / g);
            csv.write("fleet_size," + n + "," + g + "," + b + "\n");
        }

        // ---------------- Experiment 2: cell size ----------------
        int n = 10000;
        double[] cellSizes = {0.25, 0.5, 1, 2, 5, 10, 25, 50};
        System.out.println();
        System.out.println("Experiment 2: effect of cell size (n = " + n + ")");
        System.out.println("Cell size (km) | Grid ms/query");
        System.out.println("---------------+--------------");

        double[] px = new double[n];
        double[] py = new double[n];
        for (int i = 0; i < n; i++) {
            px[i] = rnd.nextDouble() * CITY;
            py[i] = rnd.nextDouble() * CITY;
        }
        for (int c = 0; c < cellSizes.length; c++) {
            SpatialHashGrid grid = new SpatialHashGrid(cellSizes[c]);
            for (int i = 0; i < n; i++) {
                grid.insert(new Ambulance(i, px[i], py[i]));
            }
            timeGrid(grid, qx, qy);                  // warm-up
            double g = timeGrid(grid, qx, qy);
            System.out.printf("%14.2f | %13.4f%n", cellSizes[c], g);
            csv.write("cell_size," + cellSizes[c] + "," + g + ",\n");
        }

        csv.close();
        System.out.println();
        System.out.println("Saved results.csv   (checksum " + checksum + ")");
    }

    // Very small timings are noisy, so each measurement is repeated and we
    // keep the BEST (fastest) run - the one least disturbed by other programs.
    static final int REPEATS = 5;

    /** Average milliseconds per query for the grid. */
    static double timeGrid(SpatialHashGrid grid, double[] qx, double[] qy) {
        double best = Double.MAX_VALUE;
        for (int r = 0; r < REPEATS; r++) {
            long start = System.nanoTime();
            for (int i = 0; i < qx.length; i++) {
                Ambulance a = grid.nearest(qx[i], qy[i]);
                checksum += a.id;
            }
            long end = System.nanoTime();
            double ms = (end - start) / 1e6 / qx.length;
            if (ms < best) best = ms;
        }
        return best;
    }

    /** Average milliseconds per query for brute force. */
    static double timeBrute(BruteForceFinder brute, double[] qx, double[] qy) {
        double best = Double.MAX_VALUE;
        for (int r = 0; r < REPEATS; r++) {
            long start = System.nanoTime();
            for (int i = 0; i < qx.length; i++) {
                Ambulance a = brute.nearest(qx[i], qy[i]);
                checksum += a.id;
            }
            long end = System.nanoTime();
            double ms = (end - start) / 1e6 / qx.length;
            if (ms < best) best = ms;
        }
        return best;
    }
}
