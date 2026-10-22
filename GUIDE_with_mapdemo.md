# Golden-Hour Ambulance Dispatch System (Java)
### DSA Case Study: Spatial Hash Grid

---

## PART A. THE CASE STUDY

### 1. Problem statement
In a medical emergency, the first hour (the "golden hour") decides survival. A control room must find the **nearest available ambulance** to an accident within milliseconds, while hundreds or thousands of ambulances keep moving and changing status (available / busy).

Checking every ambulance for every accident is **O(n)** per query. That does not scale for a large city fleet or for many accidents at once.

**Aim:** build a dispatch engine whose nearest-ambulance lookup time stays almost constant as the fleet grows.

### 2. Data structure chosen: Spatial Hash Grid
Cut the city into equal square cells. Store the grid as a **HashMap**:

```
key   = a number identifying the cell (its column and row)
value = ArrayList of the ambulances inside that cell
```

Only non-empty cells are stored. To find the nearest ambulance, look in the accident's own cell, then in the ring of cells around it, and so on outward. Stop once no unseen cell can possibly contain anything closer.

### 3. Why this and not a Quadtree / K-d Tree?
Ambulances **move constantly** (GPS pings every few seconds).

| | Spatial Hash Grid | Quadtree / K-d Tree |
|---|---|---|
| Update a moving ambulance | O(1): usually just change coordinates; at most one cell change | delete + re-insert, may need re-balancing / node splitting |
| Insert / remove | O(1) average | O(log n) |
| Nearest query | about O(1) with a good cell size | O(log n) |
| Structure | flat, easy to code and debug | pointer-heavy, complex |
| Weakness | needs a sensible cell size; clustered data crowds a few cells | handles uneven density better |

Conclusion: for *many points that keep moving*, a flat grid is simpler and cheaper to maintain. Trees win when data is static or very unevenly spread.

### 4. Operations and complexity
| Operation | Cost |
|---|---|
| `insert` | O(1) average |
| `remove` | O(1) average (plus scanning one small cell list) |
| `move` (GPS update) | O(1) average |
| `nearest(x, y)` | about O(1) for a well-chosen cell size, O(n) worst case |
| Space | O(n) |
| Brute force `nearest` (baseline) | O(n) |

### 5. Results (`java Benchmark`, 3000 queries, best of 5 runs)
Cell size = city width / sqrt(n).

| Ambulances | Grid (ms/query) | Brute force (ms/query) | Speed-up |
|---|---|---|---|
| 100 | 0.0003 | 0.0003 | 1x |
| 1,000 | 0.0004 | 0.0019 | 5x |
| 10,000 | 0.0003 | 0.0194 | 62x |
| 50,000 | 0.0008 | 0.2812 | 352x |

The grid stays almost flat while brute force grows with the fleet (`chart_query_time.png`). For very small fleets (about 100) there is no benefit, which is expected: the grid pays off when the fleet is large. Exact timings differ from machine to machine, but the *shape* of the graph will be the same.

**Cell-size experiment** (n = 10,000): 0.5 km cells = 0.0003 ms, 5 km = 0.0018 ms, 25 km = 0.0191 ms, 50 km (one giant cell) = 0.0197 ms. Huge cells degrade to brute force, so cell size is the key tuning parameter (`chart_cell_size.png`).

### 6. Correctness
`Tests.java` runs 300 random scenarios (6,000 queries: random fleet sizes, cell sizes, moves, removals, negative coordinates, points exactly on cell borders) and checks that the grid returns **exactly the same distance as brute force**. Result: all 11,625 checks pass.

### 7. Limitations (say these yourself, it earns trust)
- Uses straight-line distance on a flat map. Real dispatch uses road distance and traffic. Extension: use the grid to shortlist the nearest few ambulances, then run Dijkstra on a road graph for the true ETA.
- Returns only the single nearest ambulance. Extension: return the nearest k.
- Very clustered fleets (many ambulances at one hospital) crowd a few cells. Extension: adaptive or hierarchical grids.
- Cell size is fixed. Extension: tune it automatically from fleet density.

---

## PART B. HOW THE PROJECT WAS BUILT, STEP BY STEP

### Files
| File | What it is |
|---|---|
| `Ambulance.java` | One ambulance: id, location, available or busy |
| `SpatialHashGrid.java` | **The data structure** (the most important file) |
| `BruteForceFinder.java` | The simple O(n) version, used for testing and comparison |
| `DispatchResult.java` | Small class holding the result of one dispatch |
| `DispatchSystem.java` | The application: fleet, status, dispatch, ETA |
| `Main.java` | Live demo with text map |
| `Tests.java` | Correctness tests |
| `Benchmark.java` | Speed comparison, saves `results.csv` |
| `MapExport.java` | Saves the demo scene to `map_data.csv`; `map_demo.png` is a picture of exactly that scene |

Java concepts used: classes and objects, `HashMap`, `ArrayList`, loops, `if/else`, arrays. Nothing else.

### Step 1. Turn a location into a cell
```java
int col = (int) Math.floor(x / cellSize);
int row = (int) Math.floor(y / cellSize);
```
With 2 km cells, the point (5.3, 9.9) is in column 2, row 4. We then combine them into one number to use as the HashMap key:
```java
long key = (long) col * 1000003L + row;
```
This is the "hash" part: location in, bucket out. (`Math.floor` rather than a plain cast matters for negative coordinates, because `(int) -0.5` gives 0 but we need -1.)

### Step 2. Store ambulances in a HashMap of lists
`cells.get(key)` returns the list of ambulances in that cell, or `null` if the cell is empty. `insert` finds the cell and adds the ambulance. `remove` takes it out and deletes the cell if it became empty.

### Step 3. Handle movement (`move`)
Work out the old cell and the new cell. If they are the same, only update the coordinates. Otherwise take the ambulance out of the old list and add it to the new one. This is the reason the grid suits moving objects.

### Step 4. Find the nearest ambulance (`nearest`), the main idea
1. Find the accident's cell. Check the ambulances in it (ring 0).
2. Check the 8 cells around it (ring 1), then the next ring (ring 2), and so on.
3. **Stop rule:** after finishing ring r, any unseen ambulance is in ring r+1 or beyond, so it is at least `r × cellSize` away. If our best distance is already at most that, nothing closer can exist, so we stop. The answer is exact.

Why we cannot stop at the first ambulance found: an accident near a cell border may have a closer ambulance just across the border in the next cell. This is the most likely viva question.

### Step 5. Build the dispatch layer (`DispatchSystem.java`)
- Only **available** ambulances live in the grid. When one is dispatched it is removed from the grid and marked busy, so a search can never pick it. When it finishes, `completeJob` puts it back.
- `dispatch(x, y)` finds the nearest, removes it, and computes ETA = distance / speed (40 km/h assumed). It flags whether the ETA is within a 10-minute target.

### Step 6. Prove it is correct (`Tests.java`)
Compare against the brute force on random data. Brute force is obviously correct, so if the grid agrees in every case we can trust it. Each finder gets its **own copy** of every ambulance so that one cannot accidentally change the other's data.

### Step 7. Prove it is fast (`Benchmark.java`)
The same data and queries go to both versions, timed with `System.nanoTime()`. Because Java optimises code as it runs, the benchmark does a warm-up run first and then keeps the best of 5 runs. Mention this if asked why the benchmark looks the way it does. Challenge I hit while building: an early version recomputed the city bounds on every query (an O(n) scan), which made the grid scale badly. Tracking the bounds in `insert` and `move` fixed it.

### Step 8. Make it visible (`Main.java`)
Step 6 of the demo prints the city as a text map. `X` is the accident cell, `[ ]` marks the cells the search visited, and the last line says how many ambulances it checked out of the total.

---

## PART C. HOW TO RUN IT

You need only the JDK (Java 8 or newer).

```
cd ambulance_dispatch_java
javac *.java            # compile everything
java Tests              # should print ALL TESTS PASSED
java Main               # scripted demo
java Main manual        # type accident locations yourself
java Benchmark          # speed comparison (about 1 minute)
java MapExport          # saves the Step 6 scene to map_data.csv
```

The charts and `map_demo.png` were drawn from `results.csv` and `map_data.csv` with a plotting tool (matplotlib). The data in them comes from the Java program. You do not need to redraw them to run the project; you can also show the text map in the terminal.

---

## PART D. HOW TO DEMONSTRATE IT (about 7 minutes)

### Before the evaluators arrive
Compile, run `java Tests` once, and have `chart_query_time.png` and `chart_cell_size.png` open. Keep `SpatialHashGrid.java` open in your editor.

### Suggested flow

**1. The problem (45 sec).** "In an emergency every minute counts. If a city has 50,000 ambulances and we check all of them for every accident, that is 50,000 distance calculations per call. I wanted a structure that finds the nearest ambulance without looking at all of them."

**2. The idea, using the text map (1.5 min).** Run `java Main` and scroll to Step 6. "I divide the city into cells. X is the accident. I only searched the cells in brackets. Out of 60 ambulances I checked 5, and still found the true nearest."

**3. How it is stored (1 min).** In `SpatialHashGrid.java` show `colOf`, `rowOf`, `keyOf` and the `cells` HashMap. "Location goes in, cell comes out. Each cell holds a list of ambulances."

**4. The stop rule (1 min).** Show the `nearest` method and read the comment above it. "I search ring by ring and stop only when no unseen cell can contain anything closer, so the answer is exact, not approximate."

**5. Live demo (1.5 min).** Walk through Steps 1 to 5 of the output. Point out the "grid checked 2 of 39 free ambulances" lines. Optionally run `java Main manual` and let an evaluator type any accident location.

**6. Proof (1.5 min).** Show `java Tests` passing, then the benchmark table and `chart_query_time.png`. "Brute force grows with the fleet. The grid stays flat. At 50,000 ambulances it is hundreds of times faster."

**7. Why not a tree + limitations (1 min).** Use the table in section 3 and the limitations in section 7.

### Likely viva questions and answers

**Q: Why a grid and not a Quadtree?** Ambulances move all the time. In a grid, a move is a small HashMap update. In a tree it is delete + re-insert and possibly rebalancing.

**Q: How do you choose the cell size?** Roughly the average gap between ambulances, city width / sqrt(n). Too big and you scan many ambulances per cell (it degrades to brute force). Too small and you scan many empty cells. Show `chart_cell_size.png`.

**Q: What if the nearest ambulance is in a neighbouring cell?** That is why the search goes in rings and has a stop rule. It keeps expanding until no closer ambulance is possible.

**Q: What is the worst case?** If all ambulances sit in one cell it behaves like brute force. If almost everyone is busy, the search has to expand through many empty rings. Both are limitations.

**Q: Why is it called a hash structure?** The key is the cell number, and `HashMap` hashes it to find the bucket in O(1) average.

**Q: How does `HashMap` work?** It turns the key into a number (hash), uses that number to jump straight to a bucket, and stores the value there. That is why lookups do not depend on how many entries there are.

**Q: Is the distance real?** It is straight-line on a flat map. In practice the grid would shortlist candidates, and a graph algorithm such as Dijkstra would compute road distance.

**Q: How do you know it is correct?** 300 randomized tests against a brute-force version, including moves, removals, negative coordinates and border cases.

**Q: Time and space complexity?** Insert, remove and move are O(1) average. Nearest is about O(1) for a well-chosen cell size (O(n) worst case). Space is O(n).

### Tips
- Be ready to explain every line of `SpatialHashGrid.java`. It is short on purpose.
- If you change anything, recompile and rerun `java Tests` before presenting.
- If you do not know an answer, say what you would do to find out. That reads better than guessing.
