package agents.YBCriber;

import core.game.StateObservation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import tools.ElapsedCpuTimer;

public class Atom4SetReduced extends AtomSet{
  private Set<ArrayList<Integer>> atoms;

  public Atom4SetReduced(BfsNode node, ElapsedCpuTimer timer, int TIME_MARGIN){
    final int MAX_ATOMS_SIZE = Agent.h * Agent.w * 4;
    if (node == null){
      if (Agent.deterministic) this.atoms = new HashSet<>(MAX_ATOMS_SIZE * 2 * 700);
      else this.atoms = new HashSet<>();
      return;
    }
    this.atoms = new HashSet<>();
    if (node == null) return;
    boolean counting = (timer != null);

    // avatar atom
    int aAtom = getAvatarAtom(node) * NUM_ATOM_TYPES + 0;

    // observation atoms (missing obs and obs type together)
    ArrayList<Integer> obsAtoms = new ArrayList<>();
    ArrayList<Integer> rawMissingObsIdAtoms = getMissingObsIdAtoms(node);
    for (int rmoiAtom : rawMissingObsIdAtoms) {
      obsAtoms.add(rmoiAtom * NUM_ATOM_TYPES + 1);
    }
    ArrayList<Integer> rawObsTypeAtoms = getObsTypeAtoms(node);
    for (int rotAtom : rawObsTypeAtoms) {
      obsAtoms.add(rotAtom * NUM_ATOM_TYPES + 2);
    }

    // resource atoms
    ArrayList<Integer> resourceAtoms = new ArrayList<>();
    ArrayList<Integer> rawResourceAtoms = getResourceAtoms(node);
    for (int rrAtom : rawResourceAtoms) {
      resourceAtoms.add(rrAtom * NUM_ATOM_TYPES + 3);
    }

    // score atom
    int sAtom = getScoreAtom(node) * NUM_ATOM_TYPES + 4;

    for (int i = 0; i < obsAtoms.size(); ++i) {
      int oAtom = obsAtoms.get(i);
      if (counting) if (timer.remainingTimeMillis() < TIME_MARGIN) return;
      for (int i2 = i + 1; i2 < obsAtoms.size(); ++i2) {
        int oAtom2 = obsAtoms.get(i2);
        if (counting) if (timer.remainingTimeMillis() < TIME_MARGIN) return;
        // {avatar, object, object, object}
        for (int i3 = i2 + 1; i3 < obsAtoms.size(); ++i3) {
          int oAtom3 = obsAtoms.get(i3);
          this.atoms.add(getList(aAtom, oAtom, oAtom2, oAtom3));
          if (this.atoms.size() == MAX_ATOMS_SIZE) return;
        }

        // {avatar, score, object, object}
        this.atoms.add(getList(aAtom, sAtom, oAtom, oAtom2));
        if (this.atoms.size() == MAX_ATOMS_SIZE) return;

        if (counting) if (timer.remainingTimeMillis() < TIME_MARGIN) return;

        // {avatar, resource, object, object}
        for (int rAtom : resourceAtoms){
          this.atoms.add(getList(aAtom, rAtom, oAtom, oAtom2));
          if (this.atoms.size() == MAX_ATOMS_SIZE) return;
        }
      }

      // {avatar, score, resource, observation}
      for (int rAtom : resourceAtoms){
        this.atoms.add(getList(aAtom, sAtom, rAtom, oAtom));
        if (this.atoms.size() == MAX_ATOMS_SIZE) return;
      }
    }

    // {avatar, object, object, object}
    // {avatar, score, object, object} -
    // {avatar, resource, object, object} -
    // {avatar, score, resource, object} -
    //TODO: Use these 4? A more efficient way?
  }

  private ArrayList<Integer> getList(int i, int j, int k, int l) {
    ArrayList<Integer> aux = new ArrayList<>();
    aux.add(i);
    aux.add(j);
    aux.add(k);
    aux.add(l);
    Collections.sort(aux);
    return aux;
  }

  @Override
  boolean addAll(AtomSet as) {
    if (!(as instanceof Atom4SetReduced)) {
      return super.containsAll(as);
    }
    Atom4SetReduced a4sr = (Atom4SetReduced)as;
    return this.atoms.addAll(a4sr.atoms);
  }

  @Override
  boolean containsAll(AtomSet as) {
    if (!(as instanceof Atom4SetReduced)) {
      return super.containsAll(as);
    }
    Atom4SetReduced a4sr = (Atom4SetReduced)as;
    return this.atoms.containsAll(a4sr.atoms);
  }

  @Override
  int size() {
    return this.atoms.size();
  }
}

