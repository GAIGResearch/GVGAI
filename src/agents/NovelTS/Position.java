package agents.NovelTS;
import tools.Vector2d;
import java.util.LinkedList;
import core.game.Observation;
import core.game.StateObservation;


public class Position {
    private int numPos;

    private LinkedList<Atom> positionHistory;

    // Degrading constant when calculating position heuristic
    private static final double ALPHA = 0.5;

    // EPSILON to normalize score
    private static final double EPSILON = 0.01;

    public Position() {
        this.positionHistory = new LinkedList<Atom>();
        this.numPos = 0;
    }


    public void setPosition(Feature feature) {
        this.positionHistory.addFirst(feature.getAvatarFeatures());
        this.numPos += 1;
    }


    public double getScore(Feature feature) {
        Atom point = feature.getAvatarFeatures();
        double score = 0;
        int distance;
        boolean novelDistance = true;
        double currentAlpha = ALPHA;
        for (Atom historyPoint: positionHistory) {
            distance = compareDistance(historyPoint, point) / IWPlayer.blocksize;
            // System.out.println(IWPlayer.blocksize);
            if (distance != 0){
                score += Math.log(distance)/ Math.log(2) * currentAlpha;
            } else {
                novelDistance = false;
                if (point.equals(historyPoint)) {
                    break;
                }
            }
            currentAlpha *= ALPHA;
        }
        if (novelDistance) {
            score *= 2;
        }
        score *= EPSILON;
        return score;
    }


    private int compareDistance(Atom a1, Atom a2) {
        return Math.abs(a1.getData1() - a2.getData1()) + Math.abs(a1.getData2() - a2.getData2());
    }
}
