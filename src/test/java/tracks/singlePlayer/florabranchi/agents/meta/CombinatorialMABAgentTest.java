package tracks.singlePlayer.florabranchi.agents.meta;

import org.junit.jupiter.api.Test;

import tracks.singlePlayer.florabranchi.agents.CombinatorialMABAgent;

class CombinatorialMABAgentTest {

  @Test
  public void testAgent() {


    CombinatorialMABAgent combinatorialMABAgent = new CombinatorialMABAgent();
    System.out.println(combinatorialMABAgent);

    combinatorialMABAgent.act(50, true);

  }

}