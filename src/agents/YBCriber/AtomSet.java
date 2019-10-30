package agents.YBCriber;

import core.game.Observation;
import core.game.StateObservation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import tools.Vector2d;

public abstract class AtomSet {
  protected final static int MAX_RESOURCE_TYPES = 100;
  protected final static int NUM_ATOM_TYPES = 5; // fromAvatar not included

  private static Map<Integer, Integer> avatarTypeEnc = new HashMap<>();
  private static Map<Integer, Integer> obsIdEnc = new HashMap<>();
  private static Map<Integer, Integer> obsTypeEnc = new HashMap<>();
  private static Map<Integer, Integer> resourceEnc = new HashMap<>();

  // .getAvatarAtom cache
  private static BfsNode lastArgAvatar = null;
  private static int lastResAvatar;

  // .getFromAvatarAtoms cache
  private static BfsNode lastArgFromAvatar = null;
  private static ArrayList<Integer> lastResFromAvatar;

  // .getMissingObsIdAtoms cache
  private static BfsNode lastArgMissingObsId = null;
  private static ArrayList<Integer> lastResMissingObsId;

  // .getObsTypeAtoms cache
  private static BfsNode lastArgObsType = null;
  private static ArrayList<Integer> lastResObsType;

  // .getResourceAtoms cache
  private static BfsNode lastArgResource = null;
  private static ArrayList<Integer> lastResResource;

  // .getScoreAtom cache
  private static BfsNode lastArgScore = null;
  private static int lastResScore;

  abstract int size();

  boolean addAll(AtomSet as) {
    throw new IllegalArgumentException(".addAll called with argument of wrong subclass");
  }

  boolean containsAll(AtomSet as) {
    throw new IllegalArgumentException(".containsAll called with argument of wrong subclass");
  }

  // encoded atom is in [0, NUM_TYPES * 9 * w * h)
  // < 10^5 ?
  protected static int getAvatarAtom(BfsNode node) {
    // use cached result if available
    if (node == lastArgAvatar) {
      return lastResAvatar;
    }

    StateObservation so = node.getSo();
    // TODO: add from_avatar to avatar atoms?
    Vector2d pos = so.getAvatarPosition();
    int blockSize = so.getBlockSize();
    int pX = (int)Math.round(pos.x) / blockSize;
    int pY = (int)Math.round(pos.y) / blockSize;
    Vector2d ori = so.getAvatarOrientation();
    int oX = (int)Math.round(ori.x);
    int oY = (int)Math.round(ori.y);
    if (Math.abs(oX) > 1 || Math.abs(oY) > 1) {
      System.out.println("orientation vector is too long: " + oX + " " + oY);
    }
    // not using StateObservation.getAvatarType because it is broken
    ArrayList<Observation>[][] grid = so.getObservationGrid();
    int itype = Agent.getAvatarItype(grid, pos);
    int t = getAvatarTypeEnc(itype);
    int h = Agent.h;
    int w = Agent.w;
    int avatarAtom = t;
    avatarAtom *= 3 * 3;
    avatarAtom += (oX + 1) * 3 + (oY + 1);
    avatarAtom *= w * h;
    avatarAtom += pX * h + pY;

    // cache result
    lastArgAvatar = node;
    lastResAvatar = avatarAtom;

    return avatarAtom;
  }

  protected static ArrayList<Integer> getFromAvatarAtoms(BfsNode node) {
    // use cached result if available
    if (node == lastArgFromAvatar) {
      return lastResFromAvatar;
    }

    StateObservation so = node.getSo();
    ArrayList<Integer> faAtoms = new ArrayList<>();
    ArrayList<Observation>[] faObs = so.getFromAvatarSpritesPositions();
    if (faObs == null) return faAtoms;
    int blockSize = so.getBlockSize();
    int x, y;
    for (int i = 0; i < faObs.length; ++i){
      for (Observation o : faObs[i]){
        x = ((int)o.position.x) / blockSize;
        y = ((int)o.position.y) / blockSize;
        int encPos = x * Agent.h + y;
        // Might generate a problem when a bullet advances half a cell at a time (2 consecutive states would generate the same atom)
        int faAtom = getObsTypeEnc(o.itype) * Agent.h * Agent.w + encPos;
        faAtoms.add(faAtom);
      }
    }

    // cache result
    lastArgFromAvatar = node;
    lastResFromAvatar = faAtoms;

    return faAtoms;
  }

  // encoded atoms are in [0, NUM_OBSERVATIONS)
  // < 10^5 ?
  protected static ArrayList<Integer> getMissingObsIdAtoms(BfsNode node) {
    // use cached result if available
    if (node == lastArgMissingObsId) {
      return lastResMissingObsId;
    }

    StateObservation so = node.getSo();
    Set<Integer> curObs = new HashSet<Integer>();
    ArrayList<Observation>[][] grid = so.getObservationGrid();
    for (int x = 0; x < grid.length; ++x) {
      for (int y = 0; y < grid[x].length; ++y) {
        for (Observation o : grid[x][y]) {
          curObs.add(o.obsID);
        }
      }
    }

    ArrayList<Integer> moiAtoms = new ArrayList<>();
    Set<Integer> seenObs = node.getParentSeenObs();
    for (int obsId : seenObs) {
      if (!curObs.contains(obsId)) {
        moiAtoms.add(obsId);
      }
    }

    // cache result
    lastArgMissingObsId = node;
    lastResMissingObsId = moiAtoms;

    return moiAtoms;
  }

  // encoded atoms are in [0, (NUM_TYPES + 1) * h * w)
  // < 10^5 ?
  protected static ArrayList<Integer> getObsTypeAtoms(BfsNode node) {
    // use cached result if available
    if (node == lastArgObsType) {
      return lastResObsType;
    }

    StateObservation so = node.getSo();
    ArrayList<Observation>[][] grid = so.getObservationGrid();
    ArrayList<Integer> otAtoms = new ArrayList<>();
    int type = Agent.getAvatarItype(grid, Agent.currentPos);
    int w = grid.length;
    int h = grid[0].length;
    for (int x = 0; x < grid.length; ++x) {
      for (int y = 0; y < grid[x].length; ++y) {
        int encPos = x * h + y;
        for (Observation o : grid[x][y]) {
          if (interesting(o, type)) {
            // assuming itype is unique (not just within category)
            int otAtom = getObsTypeEnc(o.itype) * h * w + encPos;
            otAtoms.add(otAtom);
          }
        }
      }
    }

    // cache result
    lastArgObsType = node;
    lastResObsType = otAtoms;

    return otAtoms;
  }

  // encoded atoms are in [0, AMOUNT_UPPER_BOUND * MAX_RESOURCE_TYPES)
  // < 10^6 ?
  protected static ArrayList<Integer> getResourceAtoms(BfsNode node) {
    // use cached result if available
    if (node == lastArgResource) {
      return lastResResource;
    }

    Map<Integer, Integer> resources = node.getSo().getAvatarResources();
    ArrayList<Integer> rAtoms = new ArrayList<>();
    for (Map.Entry<Integer, Integer> entry : resources.entrySet()) {
      int id = entry.getKey();
      int r = getResourceEnc(id);
      int amount = entry.getValue();
      int resourceAtom = amount * MAX_RESOURCE_TYPES + r;
      rAtoms.add(resourceAtom);
    }

    // cache result
    lastArgResource = node;
    lastResResource = rAtoms;

    return rAtoms;
  }

  // encoded atoms are in [0, 1000 * SCORE_UPPER_BOUND)
  // < 10^7 ?
  protected static int getScoreAtom(BfsNode node) {
    // use cached result if available
    if (node == lastArgScore) {
      return lastResScore;
    }

    StateObservation so = node.getSo();
    double score = so.getGameScore();
    // this assumes that we do not care about decimal places beyond the 3rd
    int scoreAtom = (int)Math.round(1000.0 * score);

    // cache result
    lastArgScore = node;
    lastResScore = scoreAtom;

    return scoreAtom;
  }

  private static int getAvatarTypeEnc(int itype) {
    if (!avatarTypeEnc.containsKey(itype)) {
      int siz = avatarTypeEnc.size();
      avatarTypeEnc.put(itype, siz);
    }
    return avatarTypeEnc.get(itype);
  }

  private static int getObsIdEnc(int obsId) {
    if (!obsIdEnc.containsKey(obsId)) {
      int siz = obsIdEnc.size();
      obsIdEnc.put(obsId, siz);
    }
    return obsIdEnc.get(obsId);
  }

  private static int getObsTypeEnc(int itype) {
    if (!obsTypeEnc.containsKey(itype)) {
      int siz = obsTypeEnc.size();
      obsTypeEnc.put(itype, siz);
    }
    return obsTypeEnc.get(itype);
  }

  private static int getOrientationEnc(int oX, int oY) {
    return (oX + 1) * 3 + (oY + 1);
  }

  private static int getResourceEnc(int id) {
    if (!resourceEnc.containsKey(id)) {
      int siz = resourceEnc.size();
      resourceEnc.put(id, siz);
    }
    return resourceEnc.get(id);
  }

  private static boolean interesting(Observation o, int type) {
    if (o.category == 5) return true;
    //if (Agent.movObj.contains(o.itype)) return false;
    if (!Agent.obsProperties.containsKey(type)) return true;
    if (!Agent.obsProperties.get(type).containsKey(o.itype)) return true;
    Properties p = Agent.obsProperties.get(type).get(o.itype);
    if ((p.movable > 0 || p.uMovable < 7) && !Agent.movObj.contains(o.itype)) return true;
    if (p.spawn > 0) return true;
    return false;
  }
}

