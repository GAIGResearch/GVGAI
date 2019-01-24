package agents.YBCriber;

import core.game.StateObservation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class Atom1SetReduced extends AtomSet {
  private Set<Integer> atoms;

  public Atom1SetReduced(BfsNode node) {
    this.atoms = new HashSet<>();
    if (node == null) return;

    this.atoms.add(getAvatarAtom(node) * 2);
    ArrayList<Integer> fa = getFromAvatarAtoms(node);
    for (Integer i : fa){
      this.atoms.add(i * 2 + 1);
    }
  }

  @Override
  boolean addAll(AtomSet as) {
    if (!(as instanceof Atom1SetReduced)) {
      return super.containsAll(as);
    }
    Atom1SetReduced a1sr = (Atom1SetReduced)as;
    return this.atoms.addAll(a1sr.atoms);
  }

  @Override
  boolean containsAll(AtomSet as) {
    if (!(as instanceof Atom1SetReduced)) {
      return super.containsAll(as);
    }
    Atom1SetReduced a1sr = (Atom1SetReduced)as;
    return this.atoms.containsAll(a1sr.atoms);
  }

  @Override
  int size() {
    return this.atoms.size();
  }
}

