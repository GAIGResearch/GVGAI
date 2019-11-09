package agents.YBCriber;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class Agent extends AbstractPlayer {
  static ArrayList<Types.ACTIONS> actions;
  static boolean deterministic;
  static int h;
  static int w;
  static HashMap<Integer, HashMap<Integer, Properties>> obsProperties = new HashMap<>();
  static Random random;
  static boolean oriented;
  static Vector2d currentPos;
  static int Hashpos;
  static int[][] dangerPos;
  static boolean isDet;
  static HashSet<Integer> movObj = new HashSet<Integer> ();
  static double killmin = 0.15;
  static int simDanger = 25;
  static boolean won;
  static int limit;
  static double maxsc;

  private int sims = 2;

  private Bfs bfs;
  ArrayList<Integer> candidateActionSeq;
  private int thinkingTurns;

  private StateObservation lastState;
  private int lastAction;

  static int maxTurns;
  private StateObservation stateMax = null;

  public Agent(StateObservation so, ElapsedCpuTimer timer) {
    maxTurns = 250;
    obsProperties = new HashMap<>();
    movObj = new HashSet<>();
    candidateActionSeq = new ArrayList<>();
    // does not include ACTION_NIL
    actions = so.getAvailableActions(false);
    // ACTION_NIL will have index 0
    actions.add(0, Types.ACTIONS.ACTION_NIL);

    deterministic = isDeterministic(so);
    oriented = isOriented(so);
    isDet = true;
    won = false;
    maxsc = 0;

    ArrayList<Observation>[][] obsGrid = so.getObservationGrid();
    h = obsGrid[0].length;
    w = obsGrid.length;
    Hashpos = 3 * w; //Hope map sizes don't change!!!
    random = new Random();

    // used for checking deterministic games
    lastState = null;
    lastAction = 0;

    // number of simulations at each step
    int[] nSimAtStep = {2, 1};
    if (deterministic) {
      nSimAtStep = new int[] {1};
    }
    this.bfs = new Bfs(nSimAtStep);

    this.candidateActionSeq = new ArrayList<>();
    this.thinkingTurns = 0;

    // initial BFS
    this.bfs.reset(so);
    this.bfs.run(timer);
    ++this.thinkingTurns;
  }

  public Types.ACTIONS act(StateObservation so, ElapsedCpuTimer timer) {
    // slow motion
    /*
    try {
      Thread.sleep(1000);
    } catch(InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    // */

    /*if (so.getGameTick() == 1){
      lastState = so.copy();
    }
    else if (Agent.deterministic && lastAction == 0 && so.getGameTick() > 1){
      checkDeterministic(so);
      lastState = so.copy();
    }*/
    
    //System.out.println("==================================");
	int limit = Math.max(250, maxTurns*3/4 - 100);
    testDet(so);
    if (deterministic) deterministic = isDet;
    sims = 2;
    if (isDet || deterministic) sims = 1;
    else if (dangerous(so)) sims = 10;

    //DANGER PART!!!!
    doDanger(so);
    ArrayList<Integer> actionDeaths = computeDeaths(so, timer);
    computeDanger(so, actionDeaths);

    if (deterministic) updateMaxTurns(so, timer);

    double candScore = this.evalActionSeq(so, this.candidateActionSeq, timer);

    if (this.thinkingTurns == 0) {
      this.bfs.reset(so);
    }
    this.bfs.run(timer);
    double bfsScore = this.bfs.getBestScore();

    int action = random.nextInt(actions.size());
    if ((!deterministic || (won && maxsc <= 0) || so.getGameTick() > Math.min(700, limit)) &&
        (candScore > 0.0 || (bfsScore > 0.0 && this.bfs.mxDepth > 2) || !this.bfs.hasWork())) {
      // act
      if (bfsScore > candScore) {
        this.candidateActionSeq = this.bfs.getBestActionSeq();
      }
      if (!this.candidateActionSeq.isEmpty()) {
        action = this.candidateActionSeq.remove(0);
        //System.out.println("acting: " + actions.get(action));
      } else {
        //System.out.println("acting: random");
      }
      this.thinkingTurns = 0;
    } else {
      // keep thinking
      action = 0;
      ++this.thinkingTurns;
      //System.out.println("thinking: " + actions.get(action));
    }

    int minDeaths = Collections.min(actionDeaths);

    // grid BFS
    if (!deterministic && ((bfsScore <= 0 && candScore <= 0) || actionDeaths.get(action) > minDeaths)) {
      GridBFS gridBfs = new GridBFS(so);
      int gridBfsAction = gridBfs.firstStep();
      if (gridBfsAction != 0) {
        // stop thinking and discard candidate action sequence
        this.thinkingTurns = 0;
        this.candidateActionSeq.clear();
      }
      action = gridBfsAction;
      //System.out.println("grid BFS: " + actions.get(action));
    }

    // handle emergency
    //ArrayList<Double> actionScores = this.bfs.getFirstActionScores();
    if (actionDeaths.get(action) > minDeaths) {
      int emergencyAction = -1;
      for (int i = 0; i < actions.size(); ++i) {
        if (actionDeaths.get(i) == minDeaths &&
            (emergencyAction == -1)) {
          emergencyAction = i;
        }
      }
      if (emergencyAction != 0) {
        // stop thinking and discard candidate action sequence
        this.thinkingTurns = 0;
        this.candidateActionSeq.clear();
      }
      action = emergencyAction;
      //System.out.println("emergency action: " + actions.get(emergencyAction));
    }

    /*
    System.out.println(".............");
    for (int i = 0; i < actions.size(); ++i){
      System.out.println(actions.get(i) + "\t" + actionDeaths.get(i) + "\t" + actionScores.get(i));
    }
    System.out.println(".............");
    */

    //lastAction = action;
    //printProperties();

    //System.out.println("action: " + action + " " +  actions.get(action));
    return actions.get(action);
  }


  static public double getScore(StateObservation so, StateObservation initSo) {
    double score = (so.getGameScore() - initSo.getGameScore());
    if (so.isGameOver()) {
      Types.WINNER winner = so.getGameWinner();
      // win
      if (winner == Types.WINNER.PLAYER_WINS) {
        won = true;
        if (deterministic || initSo.getGameTick() > Math.min(maxTurns/3, 700)) return score + 1e6;
        return 0.1;
      }
      // lose
      if (winner == Types.WINNER.PLAYER_LOSES) {
        return -1e6;
      }
      // no winner
      return -0.1;
    }
    else maxsc = Math.max(score, maxsc);

    Map<Integer, Integer> res = so.getAvatarResources();
    Map<Integer, Integer> initRes = initSo.getAvatarResources();

    // resources gained
    for (Map.Entry<Integer, Integer> r : res.entrySet()) {
      int amount = r.getValue();
      int initAmount = 0;
      if (initRes.containsKey(r.getKey())) {
        initAmount = initRes.get(r.getKey());
      }
      if (amount > initAmount) {
        score += 0.1 * (double)(amount - initAmount);
      }
    }

    // resources lost
    for (Map.Entry<Integer, Integer> iR : initRes.entrySet()) {
      int initAmount = iR.getValue();
      int amount = 0;
      if (res.containsKey(iR.getKey())) {
        amount = res.get(iR.getKey());
      }
      if (amount < initAmount) {
        score += 0.01 * (double)(amount - initAmount);
      }
    }

    return score;
  }

  private boolean dangerous(StateObservation so){
    ArrayList<Observation> [][] grid = so.getObservationGrid();
    Vector2d pos = getAvatarGridPosition(so);
    int type = getAvatarItype(grid, pos);
    for (int i = (int)pos.x - 2; i <= (int)pos.x + 2; ++i){
      for (int j = (int)pos.y - 2; j <= (int)pos.y + 2; ++j){
        if (i < 0 || j < 0 || i >= w || j >= h) continue;
        for (Observation o : grid[i][j]){
          if (o.itype == type) continue;
          if (!obsProperties.containsKey(type)) return true;
          if (!obsProperties.get(type).containsKey(o.itype)) return true;
          Properties p = obsProperties.get(type).get(o.itype);
          if ((p.uKill < 10 || p.kill > killmin) && movObj.contains(o.itype)) return true;
        }
      }
    }
    return false;
  }

  private boolean isDeterministic(StateObservation so) {
    so = so.copy();
    for (int i = 0; i < 17; ++i) {
      so.advance(actions.get(0));
    }
    AtomSet as = new Atom1Set(new BfsNode(so));
    StateObservation futureSo = so.copy();
    for (int i = 0; i < 27; ++i) {
      futureSo.advance(actions.get(0));
      AtomSet futureAs = new Atom1Set(new BfsNode(futureSo));
      if (!as.containsAll(futureAs) || !futureAs.containsAll(as)) {
        return false;
      }
    }
    return true;
  }

  private double evalActionSeq(StateObservation so, ArrayList<Integer> actionSeq, ElapsedCpuTimer timer) {
    if (actionSeq.isEmpty()) return -1e9;
    StateObservation futureSo = so.copy();
    for (int a : actionSeq) {
      futureSo.advance(actions.get(a));
      if (timer.remainingTimeMillis() < 10) break;
    }
    double scoreSeq =  getScore(futureSo, so);
    if (deterministic || scoreSeq > 0 || actionSeq.size() <= 2) return scoreSeq;
    // if the sequence is short don't do the mini BFS
    // perform a mini BFS starting in the last state
    int[] nSimAtDepth = {1};
    Bfs miniBFS = new Bfs(nSimAtDepth);
    miniBFS.reset(futureSo);
    ElapsedCpuTimer miniBfsTimer = new ElapsedCpuTimer();
    miniBfsTimer.setMaxTimeMillis(timer.remainingTimeMillis() - 15);
    miniBFS.run(miniBfsTimer);
    double scoreBFS = miniBFS.getBestScore();
    ArrayList<Integer> extraActions = miniBFS.getBestActionSeq();
    if (extraActions.size() > 0) {
      this.candidateActionSeq.add(extraActions.get(0));
    }
    return scoreSeq + scoreBFS;
  }

  private void computeDanger(StateObservation so, ArrayList<Integer> actionDeaths) {
    int minDeaths = Collections.min(actionDeaths);
    //ArrayList<Double> actionScores = this.bfs.getFirstActionScores();
    for (int i = 0; i < actions.size(); ++i) {
      if (actionDeaths.get(i) > minDeaths) {
        Vector2d posdang = simNoOri(getAvatarGridPosition(so), actions.get(i));
        dangerPos[(int)posdang.x][(int)posdang.y] = 1;
      }
    }
  }

  private ArrayList<Integer> computeDeaths(StateObservation so, ElapsedCpuTimer timer){
    ArrayList<Integer> actionDeaths = new ArrayList<>();
    ArrayList<Vector2d> positions = new ArrayList<>();
    ArrayList<Integer> numSims = new ArrayList<>();
    ArrayList<Integer> tests = new ArrayList<>();
    while (actionDeaths.size() < actions.size()) {
      actionDeaths.add(0);
      positions.add(null);
      numSims.add(sims);
      tests.add(0);
    }
    for (int j = 0; j < simDanger && timer.remainingTimeMillis() > 10; ++j){
      for (int i = 0; i < actions.size() && timer.remainingTimeMillis() > 10; ++i){
        if (j >= numSims.get(i)) continue;
        tests.set(i, tests.get(i) + 1);
        StateObservation nextSo = so.copy();
        nextSo.advance(actions.get(i));
        analyzeStates(so, nextSo, actions.get(i));
        Vector2d posini = so.getAvatarPosition();
        Vector2d posend = nextSo.getAvatarPosition();
        if (!nextSo.isGameOver()){
          Vector2d v = getAvatarGridPosition(nextSo);
          positions.set(i, v);
          if (v.x >= 0 && v.y >= 0) { //FK Missilecommand
            if (dangerPos[(int)v.x][(int)v.y] == 1) numSims.set(i, simDanger);
          }
        }
        if (reallyBadState(nextSo)){
          actionDeaths.set(i, actionDeaths.get(i) + 1);
        } else if (!nextSo.isGameOver() && oriented) {
          StateObservation nextSo2 = nextSo.copy();
          nextSo.advance(actions.get(i));
          analyzeStates(nextSo2, nextSo, actions.get(i));
          if (reallyBadState(nextSo)) {
            nextSo2.advance(actions.get(0));
            if (reallyBadState(nextSo2)){
              actionDeaths.set(i, actionDeaths.get(i) + 1);
            }
          }
        }
      }
    }

    /*for (int i = 0; i < actions.size(); ++i){
      if (numSims.get(i) == simDanger && tests.get(i) > 0) actionDeaths.set(i, (actionDeaths.get(i)*sims + tests.get(i) - 1)/tests.get(i));
    }*/

    /*Vector2d pos = getAvatarGridPosition(so);
    for (int i = 0; i < actions.size(); ++i){
      Vector2d pos2 = positions.get(i);
      if (pos2 == null) pos2 = sim(pos, so.getAvatarOrientation(), actions.get(i));
      if (dangerPos[(int)pos2.x][(int)pos2.y] == 1) {
        actionDeaths.set(i, actionDeaths.get(i)+1);
      }
    }*/
    return actionDeaths;
  }

  void doDanger(StateObservation so){
    ArrayList<Observation>[][] grid = so.getObservationGrid();
    int type = getAvatarItype(grid, getAvatarGridPosition(so));
    dangerPos = new int[grid.length][grid[0].length];
    if (!obsProperties.containsKey(type)) return;
    int block = so.getBlockSize();
    for (int x = 0; x < grid.length; ++x){
      for (int y = 0; y < grid[0].length; ++y){
        for (Observation o : grid[x][y]){
          if (!obsProperties.get(type).containsKey(o.itype)) continue;
          Properties p = obsProperties.get(type).get(o.itype);
          if (p.kill < killmin) continue;
          Vector2d pos = getObservationPositionGrid(o.position, block);
          for (int t : p.M.keySet()){
            Vector2d v = Properties.decode(t);
            v.x += pos.x;
            v.y += pos.y;
            v.x = Math.max(v.x, 0.0);
            v.x = Math.min(grid.length-1, v.x);
            v.y = Math.max(v.y, 0.0);
            v.y = Math.min(grid[0].length-1, v.y);
            dangerPos[(int)v.x][(int)v.y] = 1;
          }
          /*for (int t1 : p.M.keySet()){
            for (int t2 : p.M.keySet()){
              Vector2d v1 = Properties.decode(t1);
              Vector2d v2 = Properties.decode(t2);
              v1.x += v2.x + pos.x;
              v1.y += v2.y + pos.y;
              v1.x = Math.max(v1.x, 0.0);
              v1.x = Math.min(grid.length-1, v1.x);
              v1.y = Math.max(v1.y, 0.0);
              v1.y = Math.min(grid[0].length-1, v1.y);
              if (dangerPos[(int)v1.x][(int)v1.y] == 0) dangerPos[(int)v1.x][(int)v1.y] = 2;
            }
          }*/
        }
      }
    }
  }

  private void testDet(StateObservation so){
    int n = actions.size();
    int i = random.nextInt(n);
    StateObservation so1 = so.copy();
    StateObservation so2 = so.copy();
    so1.advance(actions.get(i));
    so2.advance(actions.get(i));
    HashMap<Integer, Vector2d> M1 = new HashMap<Integer, Vector2d> ();
    int block = so.getBlockSize();
    ArrayList<Observation> [][] grid = so1.getObservationGrid();
    for (int x = 0; x < grid.length; ++x){
      for (int y = 0; y < grid[0].length; ++y){
        for (Observation o : grid[x][y]){
          M1.put(o.obsID, getObservationPositionGrid(o.position, block));
        }
      }
    }
    grid = so2.getObservationGrid();
    for (int x = 0; x < grid.length; ++x){
      for (int y = 0; y < grid[0].length; ++y){
        for (Observation o : grid[x][y]){
          if (!M1.containsKey(o.obsID)) isDet = false;
          else{
            if (!M1.get(o.obsID).equals(getObservationPositionGrid(o.position, block))){
              isDet = false;
              movObj.add(o.itype);
            }
          }
        }
      }
    }
  }

  private boolean isOriented(StateObservation so) {
    Vector2d ori = so.getAvatarOrientation();
    if (ori.x == 0 && ori.y == 0) return false;

    StateObservation soc = so.copy();
    Vector2d posini = so.getAvatarPosition();
    int cont = 0;
    for (int i = 0; i < actions.size(); ++i) {
      so.advance(actions.get(i));
      Vector2d posend = so.getAvatarPosition();
      if (posini.x != posend.x || posini.y != posend.y) ++cont;
      posini = posend;
    }
    posini = soc.getAvatarPosition();
    for (int i = actions.size() - 1; i >= 0; --i) {
      soc.advance(actions.get(i));
      Vector2d posend = soc.getAvatarPosition();
      if (posini.x != posend.x || posini.y != posend.y) ++cont;
      posini = posend;
    }
    if (cont >= 2) return false;
    return true;
  }

  static boolean reallyBadState(StateObservation so) {
    if (!so.isGameOver()) return false;
    return so.getGameWinner() != Types.WINNER.PLAYER_WINS;
  }

  //De momento no hay resources!!
  static void analyzeStates(StateObservation prev, StateObservation next, Types.ACTIONS act) {
    Vector2d posPrev = getAvatarGridPosition(prev);
    Vector2d posNext = getAvatarGridPosition(next);
    ArrayList<Observation>[][] objNext = next.getObservationGrid();
    ArrayList<Observation>[][] objPrev = prev.getObservationGrid();
    int typePrev = getAvatarItype(objPrev, posPrev);
    if (!obsProperties.containsKey(typePrev)) {
      // HashMap type --> Properties
      obsProperties.put(typePrev, new HashMap<Integer, Properties>());
    }
    int typeNext = getAvatarItype(objNext, posNext);
    boolean gameOver = next.isGameOver();
    if (prev.isGameOver()) return;
    if (typeNext != typePrev && !gameOver) return;

    Vector2d posNextIntended = sim(posPrev, prev.getAvatarOrientation(), act);
    if (posPrev.x == posNextIntended.x && posPrev.y == posNextIntended.y){
      if (reallyBadState(next)){
        for (Observation o : objNext[(int)posPrev.x][(int)posPrev.y]) {
          refresh(typePrev, null, null, 1, null, null, 0, o.itype, null);
        }
      }
      return;
    }

    HashSet<Integer> objNextPosIntended = new HashSet<>();
    HashSet<Integer> pushedObj = new HashSet<Integer>();
    HashSet<Integer> objPrevPos = new HashSet<Integer>();
    HashSet<Integer> missingIDs = findMissing(prev, next, typePrev);
    ArrayList<Observation> objPrevPosIntended = objPrev[(int)posNextIntended.x][(int)posNextIntended.y];
    boolean still = (posNext.x == posPrev.x && posNext.y == posPrev.y);
    for (Observation o : objNext[(int)posNextIntended.x][(int)posNextIntended.y]) {
      objNextPosIntended.add(o.obsID);
    }
    for (Observation o : objPrev[(int)posNextIntended.x][(int)posNextIntended.y]) {
      objPrevPos.add(o.obsID);
    }
    double deltaScore = next.getGameScore() - prev.getGameScore();

    if (reallyBadState(next)){
      for (Observation o : objNext[(int)posPrev.x][(int)posPrev.y]){
        if (objPrevPos.contains(o.obsID)) {
          refresh(typePrev, null, null, 1, null, null, 0, o.itype, null);
        }
      }
      for (Observation o : objNext[(int)posNextIntended.x][(int)posNextIntended.y]) {
        refresh(typePrev, null, deltaScore, 1, null, null, 0, o.itype, null);
      }
    }

    for (Observation o : objPrevPosIntended){
      Integer access = null;
      Integer destroyed = null;
      Integer kill = null;
      Integer movable = null;
      Integer resources = null;
      //int r = getResourcesGained(prev, next);
      Double score = null;
      if (missingIDs.contains(o.obsID)) destroyed = 1;
      else destroyed = 0;
      // check access
      if (objNextPosIntended.contains(o.obsID) && !gameOver){
        // If it was there before and after
        if (still) access = 1;
        else access = 0;
      }
      // check kill
      if (objNextPosIntended.contains(o.obsID) || missingIDs.contains(o.obsID)){ // the missing thing is because sometimes both disappear
        if (reallyBadState(next)) kill = 1;
        else kill = 0;
      }
      // detroyed checked in find missing
      // check score and resources (everything that is missing has this deltaScore and resources)
      if (missingIDs.contains(o.obsID)){
        movable = 0;
        score = deltaScore;
        //resources = r;
        if (access == null) access = 0; // If it was there, and I moved and it disappeared --> Accessible
        if (kill == null) kill = 0; // If it was there, and I moved and it disappeared --> Not kill
      } else {
        if (!objNextPosIntended.contains(o.obsID)) movable = 1;
        else movable = 0;
      }

      refresh(typePrev, access, deltaScore, kill, movable, resources, destroyed, o.itype, null);
    }
  }

  static void refresh(int avatarType, Integer access, Double deltaScore, Integer kill, Integer movable, Integer resources, Integer destroyed, Integer obsType, Integer spawned){
    if (!obsProperties.get(avatarType).keySet().contains(obsType)){
      obsProperties.get(avatarType).put(obsType, new Properties(access, deltaScore, kill, movable, resources, destroyed, spawned));
    } else {
      obsProperties.get(avatarType).get(obsType).update(access, deltaScore, kill, movable, resources, destroyed, spawned);
    }
  }

  static Vector2d getPosDestPushable(Vector2d posPrev, Vector2d posNext){
    double x, y;
    x = posNext.x + (posNext.x - posPrev.x);
    y = posNext.y + (posNext.y - posPrev.y);

    x = Math.max(0, x);
    x = Math.min(w-1, x);
    y = Math.max(0, y);
    y = Math.min(h-1, y);
    return new Vector2d(x, y);
  }

  static int getResourcesGained(StateObservation prev, StateObservation next){
    int rPrev = getNumberResources(prev);
    int rNext = getNumberResources(next);
    return rNext - rPrev;
  }

  static int getNumberResources(StateObservation so){
    HashMap<Integer, Integer> res = so.getAvatarResources();
    int total = 0;
    for (int key : res.keySet()) total += res.get(key);
    return total;
  }

  static HashSet<Integer> findMissing(StateObservation prev, StateObservation next, int type) {
    ArrayList<Observation>[][] objPrev = prev.getObservationGrid();
    ArrayList<Observation>[][] objNext = next.getObservationGrid();
    int blockSize = prev.getBlockSize();
    HashMap<Integer, Vector2d> IDnext = new HashMap<Integer, Vector2d>();
    HashSet<Integer> res = new HashSet<>();
    HashSet<Integer> checkedIDs = new HashSet<>();

    for (int x = 0; x < objNext.length; ++x) {
      for (int y = 0; y < objNext[x].length; ++y) {
        for (Observation o : objNext[x][y]){
          if (!IDnext.containsKey(o.obsID)){
            Vector2d pos = getObservationPositionGrid(o.position, blockSize);
            IDnext.put(o.obsID, pos);
          }
        }
      }
    }
    for (int x = 0; x < objPrev.length; ++x) {
      for (int y = 0; y < objPrev[x].length; ++y) {
        for (Observation o : objPrev[x][y]) {
          if (!checkedIDs.contains(o.obsID)){
            checkedIDs.add(o.obsID);
            if (!IDnext.containsKey(o.obsID)) {
              res.add(o.obsID);
              if (!obsProperties.get(type).containsKey(o.itype)) {
                obsProperties.get(type).put(o.itype, new Properties(null, null, null, null, null, 1, null));
              } else {
                obsProperties.get(type).get(o.itype).update(null, null, null, null, null, 1, null);
              }
            }  else {
              Vector2d pos = getObservationPositionGrid(o.position, blockSize);
              Vector2d changePos = new Vector2d(IDnext.get(o.obsID).x - pos.x, IDnext.get(o.obsID).y - pos.y);
              if (!obsProperties.get(type).containsKey(o.itype)) {
                obsProperties.get(type).put(o.itype, new Properties(changePos));
              } else {
                obsProperties.get(type).get(o.itype).updateM(changePos);
              }
            }
          }
        }
      }
    }
    for (int x = 0; x < objNext.length; ++x) {
      for (int y = 0; y < objNext[x].length; ++y) {
        for (Observation o : objNext[x][y]){
          if(!checkedIDs.contains(o.obsID)) refresh(type, null, null, null, null, null, null, o.itype, 1);
        }
      }
    }
    return res;
  }

  static Vector2d getObservationPositionGrid(Vector2d posPix, int blockSize){
    Vector2d posGrid = new Vector2d();
    posGrid.x = ((int)posPix.x) / blockSize;
    posGrid.y = ((int)posPix.y) / blockSize;
    return posGrid;
  }

  static Vector2d sim(Vector2d pos, Vector2d ori, Types.ACTIONS act) {
    Vector2d posInt = pos.copy();
    int[] xv = {0, -1, 0, 1, 0};
    int[] yv = {1, 0, -1, 0, 0};
    int i = -1;
    if (act.toString().toLowerCase().contains("down")) i = 0;
    else if (act.toString().toLowerCase().contains("left")) i = 1;
    else if (act.toString().toLowerCase().contains("up")) i = 2;
    else if (act.toString().toLowerCase().contains("right")) i = 3;
    else i = 4;
    if (!oriented) {
      posInt.x += xv[i];
      posInt.y += yv[i];
    } else {
      posInt.x += (xv[i] + (int)ori.x)/2;
      posInt.y += (yv[i] + (int)ori.y)/2;
    }
    posInt.x = Math.max(0, posInt.x);
    posInt.x = Math.min(w-1, posInt.x);
    posInt.y = Math.max(0, posInt.y);
    posInt.y = Math.min(h-1, posInt.y);
    return posInt;
  }

  static Vector2d simNoOri(Vector2d pos, Types.ACTIONS act) {
    Vector2d posInt = pos.copy();
    int[] xv = {0, -1, 0, 1, 0};
    int[] yv = {1, 0, -1, 0, 0};
    int i = -1;
    if (act.toString().toLowerCase().contains("down")) i = 0;
    else if (act.toString().toLowerCase().contains("left")) i = 1;
    else if (act.toString().toLowerCase().contains("up")) i = 2;
    else if (act.toString().toLowerCase().contains("right")) i = 3;
    else i = 4;
    posInt.x += xv[i];
    posInt.y += yv[i];
    posInt.x = Math.max(0, posInt.x);
    posInt.x = Math.min(w-1, posInt.x);
    posInt.y = Math.max(0, posInt.y);
    posInt.y = Math.min(h-1, posInt.y);
    return posInt;
  }

  static int getAvatarItype(ArrayList<Observation>[][] grid, Vector2d pos) {
    int posX = (int) pos.x;
    int posY = (int) pos.y;

    if (posX < 0 || posY < 0 || posX >= grid.length || posY >= grid[0].length) return 0;
    ArrayList<Observation> obsPosAvatar = grid[posX][posY];
    Observation auxObs;
    int avatarCat = 0;

    for (int k = 0; k < obsPosAvatar.size(); k++) {
      auxObs = obsPosAvatar.get(k);
      if (auxObs.category == avatarCat) return auxObs.itype;
    }
    return 0;
  }

  static Vector2d getAvatarGridPosition(StateObservation so) {
    Vector2d v = new Vector2d();
    int factor = so.getBlockSize();
    v.x = ((int)so.getAvatarPosition().x) / factor;
    v.y = ((int)so.getAvatarPosition().y) / factor;
    return v;
  }

  private void printProperties() {
    for (int key1 : obsProperties.keySet()){
      System.out.println("-------------------------------------");
      System.out.println(key1);
      System.out.println("Key\t\tValue\t\tNumber updated");
      for (int key : obsProperties.get(key1).keySet()) {
        System.out.println("______________");
        System.out.println(key);
        Properties p = obsProperties.get(key1).get(key);
        System.out.printf("Sco:\t%.3f\t\t%d%n", p.score, p.uScore);
        System.out.printf("Kil:\t%.3f\t\t%d%n", p.kill, p.uKill);
        System.out.printf("Des:\t%.3f\t\t%d%n", p.destroyed, p.uDestroyed);
        System.out.printf("Acc:\t%.3f\t\t%d%n", p.access, p.uAccess);
        System.out.printf("Res:\t%.3f\t\t%d%n", p.resources, p.uResources);
        System.out.printf("Mov:\t%.3f\t\t%d%n", p.movable, p.uMovable);
        System.out.println(p.spawn);
      }
    }
  }

  private void checkDeterministic(StateObservation currState){
    Agent.deterministic = statesAreEqual(currState, lastState);
  }

  private boolean statesAreEqual(StateObservation currState, StateObservation lastState){
    Set<Integer> currObj = new HashSet<Integer>();
    ArrayList<Observation>[][] currGrid = currState.getObservationGrid();
    ArrayList<Observation>[][] lastGrid = lastState.getObservationGrid();
    ArrayList<Observation> currCell;
    ArrayList<Observation> lastCell;

    for (int i = 0; i < currGrid.length; ++i){
      for (int j = 0; j < currGrid[i].length; ++j){
        lastCell = lastGrid[i][j];
        currCell = currGrid[i][j];
        if (lastCell.size() != currCell.size()){
          return false;
        }
        for (int k = 0; k < currCell.size(); ++k) currObj.add(currCell.get(k).itype);
        for (int k2 = 0; k2 < lastCell.size(); ++k2) if (!currObj.contains(lastCell.get(k2).itype)) return false;
        currObj.clear();
      }
    }
    return true;
  }

  public void updateMaxTurns(StateObservation so, ElapsedCpuTimer timer){
    if (stateMax == null) stateMax = so.copy();
    for (int i = 0; i < 35 && timer.remainingTimeMillis() > 25; ++i){
      stateMax.advance(actions.get(0));
      if (!stateMax.isGameOver()) maxTurns = Math.max(stateMax.getGameTick(), maxTurns);
      else return;
    }
  }
}

