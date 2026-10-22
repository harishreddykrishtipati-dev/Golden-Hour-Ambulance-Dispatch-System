import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * MapExport.java
 * Rebuilds the SAME scene as Step 6 of Main (same seed, same ambulances,
 * same accident) and saves it to map_data.csv, so a picture of exactly that
 * scene can be drawn.
 * Run:  java MapExport
 */
public class MapExport {
    public static void main(String[] args) throws IOException {
        SpatialHashGrid grid = new SpatialHashGrid(3.0);
        Random rnd = new Random(11);
        Ambulance[] all = new Ambulance[60];
        for (int i = 0; i < 60; i++) {
            all[i] = new Ambulance(i, rnd.nextDouble() * 30, rnd.nextDouble() * 30);
            grid.insert(all[i]);
        }

        double ax = 18.2, ay = 12.4;
        Ambulance best = grid.nearest(ax, ay);

        FileWriter out = new FileWriter("map_data.csv");
        out.write("cell_size," + grid.cellSize + "\n");
        out.write("accident," + ax + "," + ay + "\n");
        out.write("nearest," + best.id + "," + best.x + "," + best.y + ","
                + best.distanceTo(ax, ay) + "\n");
        out.write("rings," + grid.lastRings + "\n");
        out.write("checked," + grid.lastChecked + "\n");
        for (int i = 0; i < all.length; i++) {
            out.write("amb," + all[i].id + "," + all[i].x + "," + all[i].y + "\n");
        }
        out.close();
        System.out.println("Saved map_data.csv  (checked " + grid.lastChecked
                + " of " + grid.count + " ambulances)");
    }
}
