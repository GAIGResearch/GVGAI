package tracks.singlePlayer.florabranchi.test;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ontology.Types;

public class MockStateObservation {

  public List<Types.ACTIONS> actions = new ArrayList<>();

  public Random rand = new Random();

  public double maxReward = 10;
  public double minReward = -1;
  public double terminalStateProb = 10;

  {
    this.actions.add(Types.ACTIONS.ACTION_USE);
    this.actions.add(Types.ACTIONS.ACTION_DOWN);
    this.actions.add(Types.ACTIONS.ACTION_LEFT);
    this.actions.add(Types.ACTIONS.ACTION_RIGHT);
  }

  public List<Types.ACTIONS> getAvailableActions() {
    return actions;
  }

  public MockStateObservation() {
  }

  public double getReward(final Types.ACTIONS action) {
    return rand.nextInt((int) maxReward);
  }

  public boolean isTerminalState() {
    return rand.nextInt(100) <= terminalStateProb;
  }


  public MockStateObservation copy() {
    return new MockStateObservation();
  }

  public void advance(final Types.ACTIONS actions) {

  }
}
