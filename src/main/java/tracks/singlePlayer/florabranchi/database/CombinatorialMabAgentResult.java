package tracks.singlePlayer.florabranchi.database;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cmab_mcts_results")
public class CombinatorialMabAgentResult implements Serializable {

  @Id
  @GeneratedValue
  public int id;

  public String agent;

  public String gameName;

  public int gameLevel;

  public double finalScore;

  public boolean won;

  public boolean treeReuse;
  public boolean rawGameScore;
  public boolean macroActions;
  public boolean lossAvoidance;
  public boolean earlyInitialization;
  public boolean selectHighestScoreChild;
  public boolean shallowRollout;

  private final String timestamp;

  public CombinatorialMabAgentResult() {
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    timestamp = dateFormat.format(new Date(System.currentTimeMillis()));
  }

}
