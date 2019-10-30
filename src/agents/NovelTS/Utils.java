package agents.NovelTS;

public class Utils {

    public static int compareDistance(Atom a1, Atom a2) {
        return Math.abs(a1.getData1() - a2.getData1()) + Math.abs(a1.getData2() - a2.getData2());
    }
}
