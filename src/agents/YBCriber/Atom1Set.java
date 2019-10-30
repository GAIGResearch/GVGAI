package agents.YBCriber;

import core.game.StateObservation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Atom1Set extends AtomSet {
  private Set<Integer> atoms;

  public Atom1Set(BfsNode node) {
    this.atoms = new HashSet<>();
    if (node == null) return;

    // avatar (atom type 0)
    this.atoms.add(getAvatarAtom(node) * NUM_ATOM_TYPES + 0);

    // missing observations (atom type 1)
    ArrayList<Integer> missingObsIdAtoms = getMissingObsIdAtoms(node);
    for (int moiAtom : missingObsIdAtoms) {
      this.atoms.add(moiAtom * NUM_ATOM_TYPES + 1);
    }

    // observations (atom type 2)
    Agent.currentPos = Agent.getAvatarGridPosition(node.getSo());
    ArrayList<Integer> obsTypeAtoms = getObsTypeAtoms(node);
    for (int otAtom : obsTypeAtoms) {
      this.atoms.add(otAtom * NUM_ATOM_TYPES + 2);
    }

    // resources (atom type 3)
    ArrayList<Integer> resourceAtoms = getResourceAtoms(node);
    for (int rAtom : resourceAtoms) {
      this.atoms.add(rAtom * NUM_ATOM_TYPES + 3);
    }

    // score (atom type 4)
    this.atoms.add(getScoreAtom(node) * NUM_ATOM_TYPES + 4);
  }

  @Override
  boolean addAll(AtomSet as) {
    if (!(as instanceof Atom1Set)) {
      return super.containsAll(as);
    }
    Atom1Set a1s = (Atom1Set)as;
    return this.atoms.addAll(a1s.atoms);
  }

  @Override
  boolean containsAll(AtomSet as) {
    if (!(as instanceof Atom1Set)) {
      return super.containsAll(as);
    }
    Atom1Set a1s = (Atom1Set)as;
    return this.atoms.containsAll(a1s.atoms);
  }

  @Override
  int size() {
    return this.atoms.size();
  }

  public Set<Integer> getAtoms() {
	//System.out.println(atoms.size());
    return Collections.unmodifiableSet(atoms);
  }
}

