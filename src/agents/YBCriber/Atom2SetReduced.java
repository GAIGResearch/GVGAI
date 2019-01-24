package agents.YBCriber;

import core.game.StateObservation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Atom2SetReduced extends AtomSet {
  private Set<Long> atoms;

  public Atom2SetReduced(BfsNode node) {
    this.atoms = new HashSet<>();
    if (node == null) return;

    long left = (long)(getAvatarAtom(node)) << 32;

    // {avatar, missing observation} (missing obs has atom type 1)
    ArrayList<Integer> missingObsIdAtoms = getMissingObsIdAtoms(node);
    for (int moiAtom : missingObsIdAtoms) {
      long right = (long)(moiAtom * NUM_ATOM_TYPES + 1);
      this.atoms.add(left | right);
    }

    // {avatar, observation} (obs has atom type 2)
    Agent.currentPos = Agent.getAvatarGridPosition(node.getSo());
    ArrayList<Integer> obsTypeAtoms = getObsTypeAtoms(node);
    for (int otAtom : obsTypeAtoms) {
      long right = (long)(otAtom * NUM_ATOM_TYPES + 2);
      this.atoms.add(left | right);
    }

    // {avatar, resource} (resource has atom type 3)
    ArrayList<Integer> resourceAtoms = getResourceAtoms(node);
    for (int rAtom : resourceAtoms) {
      long right = (long)(rAtom * NUM_ATOM_TYPES + 3);
      this.atoms.add(left | right);
    }

    // {avatar, score} (score has atom type 4)
    int sAtom = getScoreAtom(node);
    this.atoms.add(left | (long)(sAtom * NUM_ATOM_TYPES + 4));
  }

  @Override
  boolean addAll(AtomSet as) {
    if (!(as instanceof Atom2SetReduced)) {
      return super.containsAll(as);
    }
    Atom2SetReduced a2sr = (Atom2SetReduced)as;
    return this.atoms.addAll(a2sr.atoms);
  }

  @Override
  boolean containsAll(AtomSet as) {
    if (!(as instanceof Atom2SetReduced)) {
      return super.containsAll(as);
    }
    Atom2SetReduced a2sr = (Atom2SetReduced)as;
    return this.atoms.containsAll(a2sr.atoms);
  }

  @Override
  int size() {
    return this.atoms.size();
  }
}

