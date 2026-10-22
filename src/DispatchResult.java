/** Holds the outcome of one dispatch: who was sent, how far, how long. */
public class DispatchResult {
    double accidentX;
    double accidentY;
    Ambulance ambulance;
    double distanceKm;
    double etaMinutes;
    boolean withinTarget;

    public DispatchResult(double accidentX, double accidentY, Ambulance ambulance,
                          double distanceKm, double etaMinutes, boolean withinTarget) {
        this.accidentX = accidentX;
        this.accidentY = accidentY;
        this.ambulance = ambulance;
        this.distanceKm = distanceKm;
        this.etaMinutes = etaMinutes;
        this.withinTarget = withinTarget;
    }
}
