package agents.YBCriber;

import core.game.StateObservation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import tools.ElapsedCpuTimer;

public class Bfs {
  private static final int MAX_NUM_SEEN_STATES = 22500;
  private static final int MAX_NUM_STORED_SEQUENCES = 200;
  private static final int MAX_QUEUES_TOTAL_SIZE = 12000;
  private static final int TIME_MARGIN = 5;

  private ArrayList<AtomSet> atomSets;
  // using TreeMap to keep the best sequences sorted by score
  private TreeMap<Double, Set<ArrayList<Integer>>> bestSeqs;
  private StateObservation initialSo;
  private int[] numSimulationsAtStep;
  private ArrayList<Queue<BfsNode>> queues;
  private Set<Set<Integer>> seenStates;
  private Map<ArrayList<Integer>, Double> seqScore;

  // stats
  int mxDepth;
  int numExplored;
  int numRuns;
  int[][] vis;

  // takes as argument the number of simulations that should be performed at each action.
  // nSimAtStep[0] applies to the first action, nSimAtStep[1] to the second, etc.
  // nSimAtStep[size - 1] applies to all subsequent actions
  Bfs(int[] nSimAtStep) {
    this.clear();
    this.numSimulationsAtStep = nSimAtStep;
  }

  ArrayList<Integer> getBestActionSeq() {
    if (this.bestSeqs.isEmpty()) return new ArrayList<>();
    Set<ArrayList<Integer>> tied = this.bestSeqs.lastEntry().getValue();
    ArrayList<Integer> ret = null;
    // return shortest sequence in case of tie
    for (ArrayList<Integer> seq : tied) {
      if (ret == null || seq.size() < ret.size()) {
        ret = new ArrayList<>(seq);
      }
    }
    return ret;
  }

  double getBestScore() {
    if (this.bestSeqs.isEmpty()) return -1e9;
    return this.bestSeqs.lastEntry().getKey();
  }

  ArrayList<Double> getFirstActionScores() {
    ArrayList<Double> firstActionScores = new ArrayList<>();
    while (firstActionScores.size() < Agent.actions.size()) {
      firstActionScores.add(-1e9);
    }
    for (Map.Entry<ArrayList<Integer>, Double> entry : this.seqScore.entrySet()) {
      ArrayList<Integer> seq = entry.getKey();
      if (!seq.isEmpty()) {
        int fa = seq.get(0);
        double score = entry.getValue();
        if (score > firstActionScores.get(fa)) {
          firstActionScores.set(fa, score);
        }
      }
    }
    return firstActionScores;
  }

  boolean hasWork() {
    return this.queuesTotalSize() > 0;
  }

  void printStats(boolean printVisited) {
    System.out.println("nodes explored: " + this.numExplored);
    System.out.println("nodes explored per turn: " + (double)this.numExplored / (double)this.numRuns);
    System.out.println("max depth: " + this.mxDepth);
    System.out.print("queue sizes:");
    for (Queue<BfsNode> q : this.queues) {
      System.out.print(" " + q.size());
    }
    System.out.println();
    System.out.print("atom set sizes:");
    for (AtomSet as : this.atomSets) {
      System.out.print(" " + as.size());
    }
    System.out.println();
    if (printVisited) {
      for (int i = 0; i < this.vis.length; ++i) {
        for (int j = 0; j < this.vis[i].length; ++j) {
          if (this.vis[i][j] < 10) System.out.print(" ");
          System.out.print(" ");
          System.out.print(this.vis[i][j]);
        }
        System.out.println();
      }
    }
  }

  void reset(StateObservation so) {
    this.clear();

    this.initialSo = so;
    this.queues.get(0).add(new BfsNode(this.initialSo));
  }

  void run(ElapsedCpuTimer timer) {
    this.runBfs(timer);
  }

  private void clear() {
    this.atomSets = getAtomSets(null, null);
    this.bestSeqs = new TreeMap<>();
    this.initialSo = null;
    this.queues = new ArrayList<>();
    // one queue per atom set, and one extra queue for novelty greater than the max
    while (this.queues.size() <= this.atomSets.size()) {
      this.queues.add(new LinkedList<BfsNode>());
    }
    this.seenStates = new HashSet<>();
    this.seqScore = new HashMap<>();

    // stats
    this.mxDepth = 0;
    this.numExplored = 0;
    this.numRuns = 0;
    this.vis = new int[Agent.h][Agent.w];
  }

  private void addSeqToResults(ArrayList<Integer> seq, double score) {
    this.seqScore.put(seq, score);
    if (!this.bestSeqs.containsKey(score)) {
      this.bestSeqs.put(score, new HashSet<ArrayList<Integer>>());
    }
    this.bestSeqs.get(score).add(new ArrayList<>(seq));

    // remove worst sequence if over maximum capacity
    if (this.seqScore.size() > MAX_NUM_STORED_SEQUENCES) {
      Set<ArrayList<Integer>> tied = this.bestSeqs.firstEntry().getValue();
      ArrayList<Integer> worstSeq = tied.iterator().next();
      this.removeSeqFromResults(worstSeq);
    }
  }

  private ArrayList<AtomSet> getAtomSets(BfsNode node, ElapsedCpuTimer timer) {
    // atom sets should be sorted increasingly by size (smaller tuple size first, and
    // reduced sets before non-reduced sets)
    boolean counting = (timer != null);

    ArrayList<AtomSet> a = new ArrayList<>();
    a.add(new Atom1SetReduced(node));
    a.add(new Atom1Set(node));
    a.add(new Atom2SetReduced(node));
    if (!Agent.deterministic) return a;
    if (counting) if (timer.remainingTimeMillis() < TIME_MARGIN) return a;
    //TODO: use 3 and 4 reduced only for deterministic games?
    // Comment the next two lines to stop using 3 reduced
    a.add(new Atom3SetReduced(node, timer, TIME_MARGIN));
    if (counting) if (timer.remainingTimeMillis() < TIME_MARGIN) return a;
    // Comment the next line to stop using 4 reduced
    a.add(new Atom4SetReduced(node, timer, TIME_MARGIN));
    return a;
  }

  private void runBfs(ElapsedCpuTimer timer) {
    ++this.numRuns;
    while (this.queuesTotalSize() > 0) {
      // abort if out of time
      if (timer.remainingTimeMillis() < TIME_MARGIN) break;

      int qInd = -1;
      for (int i = 0; i < this.queues.size(); ++i) {
        if (!this.queues.get(i).isEmpty()) {
          qInd = i;
          break;
        }
      }
      BfsNode node = this.queues.get(qInd).element();

      // take the worst outcome out of a bunch of simulations to account for random bad stuff.
      // always do only 1 simulation at root (depth = 0) because there is no uncertainty
      // (no actions have been performed yet)
      int numSimulations = 1;
      if (node.getDepth() > 0) {
        int step = Math.min(node.getDepth() - 1, numSimulationsAtStep.length - 1);
        numSimulations = numSimulationsAtStep[step];
      }
      node = this.worstOutcome(node, numSimulations, timer);
      StateObservation so = node.getSo();

      // update stats
      ++this.numExplored;
      if (node.getDepth() > this.mxDepth) {
        this.mxDepth = node.getDepth();
      }
      int avatarX = (int)so.getAvatarPosition().x / so.getBlockSize();
      int avatarY = (int)so.getAvatarPosition().y / so.getBlockSize();
      // apparently avatar position is sometimes invalid (e.g. in game missilecommand)
      if (avatarY >= 0 && avatarY < vis.length && avatarX >= 0 && avatarX < vis[0].length) {
        ++vis[avatarY][avatarX];
      }

      // abort if out of time
      if (timer.remainingTimeMillis() < TIME_MARGIN) break;

      // update BFS results
      double score = Agent.getScore(so, this.initialSo);
      ArrayList<Integer> seq = node.getActionSeq();
      if (!seq.isEmpty()) {
        // remove parent sequence from results
        ArrayList<Integer> parentSeq = new ArrayList<>(seq);
        parentSeq.remove(parentSeq.size() - 1);
        this.removeSeqFromResults(parentSeq);
      }
      this.addSeqToResults(seq, score);

      // continue if game is over
      if (so.isGameOver()) {
        this.queues.get(qInd).remove();
        continue;
      }

      // extract atom sets (individual atoms, pairs of atoms, triplets of atoms, etc.)
      ArrayList<AtomSet> curAtomSets = getAtomSets(node, timer);

      // abort if out of time
      if (timer.remainingTimeMillis() < TIME_MARGIN) break;

      // continue if repeated state
      // this assumes that curAtomSets.get(1) is Atom1Set
      Set<Integer> atomsState = ((Atom1Set)curAtomSets.get(1)).getAtoms();
      // TODO(felix): this does not allow usage of catapults (2 consecutive identical state obs)
      
      /*if (seenStates.contains(atomsState)){
        this.queues.get(qInd).remove();
        continue;
      }*/
      

      // abort if out of time
      if (timer.remainingTimeMillis() < TIME_MARGIN) break;

      // compute novelty of current node
      int noveltyIndex = curAtomSets.size();
      for (int i = 0; i < curAtomSets.size(); ++i) {
        AtomSet seen = this.atomSets.get(i);
        AtomSet cur = curAtomSets.get(i);
        if (!seen.containsAll(cur)) {
          noveltyIndex = i;
          break;
        }
      }

      // abort if out of time
      if (timer.remainingTimeMillis() < TIME_MARGIN) break;

      // generate children
      ArrayList<BfsNode> children = node.children();

      // make space by removing nodes from rightmost (highest novelty) queue
      int queueOverflow = this.queuesTotalSize() + children.size() - MAX_QUEUES_TOTAL_SIZE;
      for (int i = this.queues.size() - 1; queueOverflow > 0; --i) {
        Queue<BfsNode> q = this.queues.get(i);
        while (!q.isEmpty() && queueOverflow > 0) {
          q.remove();
          --queueOverflow;
        }
      }

      // enqueue children
      if (Agent.deterministic || noveltyIndex < curAtomSets.size()){
        this.queues.get(noveltyIndex).addAll(children);
      }

      // mark state as seen
      if (this.seenStates.size() < MAX_NUM_SEEN_STATES) {
        this.seenStates.add(atomsState);
      }

      // update seen atoms
      for (int i = noveltyIndex; i < curAtomSets.size(); ++i) {
        AtomSet seen = this.atomSets.get(i);
        AtomSet cur = curAtomSets.get(i);
        // TODO(felix): possibly find a better way to limit the number of atoms
        if (seen.size() < 200000) {
          seen.addAll(cur);
        }
      }

      // remove current node from queue
      this.queues.get(qInd).remove();
    }
  }

  private int queuesTotalSize() {
    int siz = 0;
    for (Queue<BfsNode> q : this.queues) {
      siz += q.size();
    }
    return siz;
  }

  private void removeSeqFromResults(ArrayList<Integer> seq) {
    if (!this.seqScore.containsKey(seq)) return;
    double score = this.seqScore.get(seq);
    this.seqScore.remove(seq);
    Set<ArrayList<Integer>> tied = this.bestSeqs.get(score);
    tied.remove(seq);
    if (tied.isEmpty()) {
      this.bestSeqs.remove(score);
    }
  }

  private BfsNode worstOutcome(BfsNode node, int numSimulations, ElapsedCpuTimer timer) {
    BfsNode worst = new BfsNode(node);
    double worstScore = Agent.getScore(worst.getNewSo(), this.initialSo);
    for (int i = 1; i < numSimulations && timer.remainingTimeMillis() > TIME_MARGIN; ++i) {
      BfsNode cur = new BfsNode(node);
      double curScore = Agent.getScore(cur.getNewSo(), this.initialSo);
      if (worst == null || curScore < worstScore) {
        worst = cur;
        worstScore = curScore;
      }
    }
    return worst;
  }
}

