package agents.YBCriber;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.LinkedList;
import java.util.Queue;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class GridBFS {
  ArrayList<Observation>[][] grid;
  int [][] visited;
  int [][] laststep;
  int [] xv = {0, -1, 0, 1};;
  int [] yv = {1, 0, -1, 0};;
  double maxscore;
  int res;
  int type;
  double newobject_score = 0.01;
  double mov_score = 0.0001;
  Vector2d posini;

  GridBFS (StateObservation so){
    this.grid = so.getObservationGrid();
    this.visited = new int [grid.length][grid[0].length];
    this.laststep = new int [grid.length][grid[0].length];
    this.maxscore = 0;
    this.res = -1;
    this.posini = Agent.getAvatarGridPosition(so);
    this.type = Agent.getAvatarItype(grid, posini);
  }

  public int firstStep(){
    Queue <Vector2d> Q = new LinkedList <Vector2d> ();
    if (Agent.actions.size() < 5) return 0;
    if (posini.x < 0 || posini.y < 0 || posini.x >= grid.length || posini.y >= grid[(int)posini.x].length) return 0;
    visited[(int)posini.x][(int)posini.y] = 1;
    laststep[(int)posini.x][(int)posini.y] = -1;
    Q.add(posini);
    while (!(Q.size() == 0)){
      Vector2d curpos = Q.remove();
      for (int i = 0; i < 4; ++i){
        if (fill(curpos, i)) Q.add(new Vector2d(curpos.x + xv[i], curpos.y + yv[i]));
      }
    }
    return findAction();
  }

  private boolean fill (Vector2d curpos, int i){
    boolean acc = true;
    int x = (int)curpos.x + xv[i];
    int y = (int)curpos.y + yv[i];
    double auxmaxscore = maxscore;
    int auxres = res;
    if (x < 0 || y < 0 || x >= grid.length || y >= grid[0].length) return false;
    if (visited[x][y] > 0 || Agent.dangerPos[x][y] == 1) return false;
    visited[x][y] = visited[(int)curpos.x][(int)curpos.y] + 1;
    if (visited[x][y] == 2) laststep[x][y] = i;
    else laststep[x][y] = laststep[(int)curpos.x][(int)curpos.y];
    for (Observation o : grid[x][y]){
      if (!Agent.obsProperties.containsKey(type)) return false;
      if (!Agent.obsProperties.get(type).containsKey(o.itype)){
        if (auxmaxscore < newobject_score/visited[x][y]){
          auxmaxscore = newobject_score/visited[x][y];
          auxres = laststep[x][y];
        }
      }
      else {
        //System.out.println("ENTRO!!");
        Properties p = Agent.obsProperties.get(type).get(o.itype);
        if (p.kill > Agent.killmin) return false;
        if (p.access > 0.8) acc = false;
        if (p.score > 0.5 && auxmaxscore < p.score/visited[x][y]){
          auxmaxscore = p.score/visited[x][y];
          auxres = laststep[(int)curpos.x][(int)curpos.y];
        }
        //Movable? Destructible?
      }
    }
    
    //System.out.println("MYSCORE: " + maxscore);
    //System.out.println("MYSTEP: " + res);
    maxscore = auxmaxscore;
    res = auxres;
    return acc;
  }

  private int findAction(){ //WTF como se cambia esto xD
    //System.out.println("MY SOLUTION :" + res);
    if (res < 0) return 0;
    if (res == 0){
      for (int i = 0; i < Agent.actions.size(); ++i){
        if (Agent.actions.get(i) == Types.ACTIONS.ACTION_DOWN) return i;
      }
    }
    if (res == 1){
      for (int i = 0; i < Agent.actions.size(); ++i){
        if (Agent.actions.get(i) == Types.ACTIONS.ACTION_LEFT) return i;
      }
    }
    if (res == 2){
      for (int i = 0; i < Agent.actions.size(); ++i){
        if (Agent.actions.get(i) == Types.ACTIONS.ACTION_UP) return i;
      }
    }
    if (res == 3){
      for (int i = 0; i < Agent.actions.size(); ++i){
        if (Agent.actions.get(i) == Types.ACTIONS.ACTION_RIGHT) return i;
      }
    }
    return 0;
  }
}

