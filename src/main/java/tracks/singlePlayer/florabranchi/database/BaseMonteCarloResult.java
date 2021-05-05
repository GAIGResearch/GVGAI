package tracks.singlePlayer.florabranchi.database;

import java.io.Serializable;
import java.util.Calendar;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "base_mcts_results")
public class BaseMonteCarloResult implements Serializable {

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

  @Temporal(TemporalType.DATE)
  private Calendar timestamp;

  public BaseMonteCarloResult() {


  }

}
